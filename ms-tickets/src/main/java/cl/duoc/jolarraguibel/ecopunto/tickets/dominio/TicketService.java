package cl.duoc.jolarraguibel.ecopunto.tickets.dominio;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de negocio de tickets. No conoce RabbitMQ.
 */
@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    private static final List<String> PALABRAS_URGENTES = List.of("rebals", "peligr", "roto", "incendio", "derram");

    private final TicketRepository repository;

    public TicketService(TicketRepository repository) {
        this.repository = repository;
    }

    /**
     * Genera el ticket de un reporte. Es idempotente: si el reporte ya tiene ticket
     * (mensaje reentregado), no crea otro y devuelve el existente.
     */
    @Transactional
    public Ticket generar(SolicitudTicket solicitud) {
        Optional<Ticket> existente = repository.findByReporteId(solicitud.reporteId());
        if (existente.isPresent()) {
            log.warn("[DUPLICADO] el reporte {} ya tiene el ticket {}; no se genera otro (messageId={})",
                    solicitud.reporteId(), existente.get().getCodigo(), solicitud.messageId());
            return existente.get();
        }

        Ticket ticket = repository.save(new Ticket(solicitud.reporteId(), solicitud.puntoLimpioId(),
                solicitud.puntoNombre(), solicitud.direccion(), solicitud.comuna(), solicitud.descripcion(),
                prioridad(solicitud.descripcion()), solicitud.solicitadoPor(), solicitud.messageId()));
        log.info("[TICKET] generado {} para reporte {} | {} ({}) | prioridad {}", ticket.getCodigo(),
                ticket.getReporteId(), ticket.getPuntoNombre(), ticket.getComuna(), ticket.getPrioridad());
        return ticket;
    }

    public List<Ticket> listar() {
        return repository.findAllByOrderByIdDesc();
    }

    public Optional<Ticket> buscar(Long id) {
        return repository.findById(id);
    }

    private static Ticket.Prioridad prioridad(String descripcion) {
        String texto = descripcion.toLowerCase(Locale.ROOT);
        return PALABRAS_URGENTES.stream().anyMatch(texto::contains) ? Ticket.Prioridad.ALTA : Ticket.Prioridad.NORMAL;
    }
}
