package cl.duoc.jolarraguibel.ecopunto.domain;

import cl.duoc.jolarraguibel.ecopunto.domain.evento.GenerarTicketMantencionCommand;
import cl.duoc.jolarraguibel.ecopunto.domain.evento.ReporteCreadoEvent;
import cl.duoc.jolarraguibel.ecopunto.domain.evento.ReporteEstadoCambiadoEvent;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lógica de reportes. Publica eventos de dominio con ApplicationEventPublisher;
 * no conoce RabbitMQ (eso vive en el paquete mensajeria).
 */
@Service
public class ReporteService {

    private static final Map<Reporte.Estado, Set<Reporte.Estado>> TRANSICIONES = Map.of(
            Reporte.Estado.PENDIENTE, Set.of(Reporte.Estado.EN_REVISION),
            Reporte.Estado.EN_REVISION, Set.of(Reporte.Estado.RESUELTO),
            Reporte.Estado.RESUELTO, Set.of());

    private final PuntoLimpioRepository puntoLimpioRepository;
    private final ReporteRepository reporteRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ReporteService(
            PuntoLimpioRepository puntoLimpioRepository,
            ReporteRepository reporteRepository,
            ApplicationEventPublisher eventPublisher) {
        this.puntoLimpioRepository = puntoLimpioRepository;
        this.reporteRepository = reporteRepository;
        this.eventPublisher = eventPublisher;
    }

    public List<Reporte> listarPorPunto(Long puntoLimpioId) {
        return reporteRepository.findByPuntoLimpioId(puntoLimpioId);
    }

    @Transactional
    public Reporte crear(Long puntoLimpioId, String descripcion, String autorEmail) {
        PuntoLimpio punto = puntoLimpioRepository.findById(puntoLimpioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Reporte reporte = reporteRepository.save(new Reporte(punto, descripcion, autorEmail));

        eventPublisher.publishEvent(new ReporteCreadoEvent(
                UUID.randomUUID().toString(), Instant.now(), reporte.getId(), punto.getId(),
                punto.getNombre(), punto.getComuna(), descripcion, autorEmail));
        return reporte;
    }

    @Transactional
    public Reporte cambiarEstado(Long reporteId, Reporte.Estado nuevo, String cambiadoPor) {
        Reporte reporte = reporteRepository.findById(reporteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Reporte.Estado anterior = reporte.getEstado();
        if (!TRANSICIONES.get(anterior).contains(nuevo)) {
            throw new TransicionInvalidaException(anterior, nuevo);
        }
        reporte.setEstado(nuevo);
        PuntoLimpio punto = reporte.getPuntoLimpio();

        eventPublisher.publishEvent(new ReporteEstadoCambiadoEvent(
                UUID.randomUUID().toString(), Instant.now(), reporte.getId(), punto.getId(),
                punto.getNombre(), punto.getComuna(), anterior.name(), nuevo.name(),
                reporte.getAutorEmail(), cambiadoPor));

        // Pasar a revisión genera una tarea para el servicio de tickets.
        if (nuevo == Reporte.Estado.EN_REVISION) {
            eventPublisher.publishEvent(new GenerarTicketMantencionCommand(
                    UUID.randomUUID().toString(), Instant.now(), reporte.getId(), punto.getId(),
                    punto.getNombre(), punto.getDireccion(), punto.getComuna(),
                    reporte.getDescripcion(), cambiadoPor));
        }
        return reporte;
    }
}
