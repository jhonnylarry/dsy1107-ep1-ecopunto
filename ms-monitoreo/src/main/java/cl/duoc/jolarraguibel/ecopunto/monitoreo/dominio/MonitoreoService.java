package cl.duoc.jolarraguibel.ecopunto.monitoreo.dominio;

import cl.duoc.jolarraguibel.ecopunto.monitoreo.mensajeria.EstadoContenedorEvent;
import cl.duoc.jolarraguibel.ecopunto.monitoreo.mensajeria.EstadoPublisher;
import cl.duoc.jolarraguibel.ecopunto.monitoreo.mensajeria.LecturaSensorEvent;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonitoreoService {

    private static final Logger log = LoggerFactory.getLogger(MonitoreoService.class);

    public record Resumen(
            long contenedores,
            Map<EstadoLlenado, Long> porEstado,
            double nivelPromedioGeneral,
            long lecturasProcesadas,
            Instant ultimaLectura) {
    }

    private final ContenedorRepository repository;
    private final EstadoPublisher estadoPublisher;
    private final MonitoreoProperties reglas;

    public MonitoreoService(ContenedorRepository repository, EstadoPublisher estadoPublisher,
            MonitoreoProperties reglas) {
        this.repository = repository;
        this.estadoPublisher = estadoPublisher;
        this.reglas = reglas;
    }

    /**
     * Aplica una lectura al contenedor. Si el estado cambia, publica el evento antes de confirmar
     * la transacción: si Kafka falla, nada se guarda y la lectura se reintenta completa
     * (puede repetirse un cambio de estado, nunca perderse).
     */
    @Transactional
    public void procesar(LecturaSensorEvent lectura) {
        Contenedor contenedor = repository.findById(lectura.contenedorId())
                .orElseGet(() -> new Contenedor(lectura.contenedorId(), lectura.puntoLimpioId(), lectura.material()));

        if (contenedor.yaConsidera(lectura.lecturaId(), lectura.medidoEn())) {
            log.info("[DUPLICADO] Lectura {} de {} ya aplicada o desactualizada; se ignora",
                    lectura.lecturaId(), lectura.contenedorId());
            return;
        }

        EstadoLlenado anterior = contenedor.getEstado();
        boolean cambio = contenedor.aplicar(lectura.lecturaId(), lectura.nivelLlenado(), lectura.medidoEn(), reglas);
        if (cambio) {
            estadoPublisher.publicar(new EstadoContenedorEvent(contenedor.getContenedorId(),
                    contenedor.getPuntoLimpioId(), contenedor.getMaterial(), contenedor.getNivelActual(),
                    contenedor.getNivelPromedio(), anterior, contenedor.getEstado(), lectura.medidoEn()));
        }
        repository.save(contenedor);
    }

    @Transactional(readOnly = true)
    public List<Contenedor> listar() {
        return repository.findAllByOrderByNivelActualDesc();
    }

    @Transactional(readOnly = true)
    public Optional<Contenedor> buscar(String contenedorId) {
        return repository.findById(contenedorId);
    }

    @Transactional(readOnly = true)
    public Resumen resumen() {
        List<Contenedor> todos = repository.findAll();
        Map<EstadoLlenado, Long> porEstado = new EnumMap<>(EstadoLlenado.class);
        for (EstadoLlenado e : EstadoLlenado.values()) {
            porEstado.put(e, 0L);
        }
        todos.forEach(c -> porEstado.merge(c.getEstado(), 1L, Long::sum));
        double promedio = todos.stream().mapToDouble(Contenedor::getNivelActual).average().orElse(0);
        long lecturas = todos.stream().mapToLong(Contenedor::getLecturas).sum();
        Instant ultima = todos.stream().map(Contenedor::getUltimaMedicion)
                .filter(java.util.Objects::nonNull).max(Instant::compareTo).orElse(null);
        return new Resumen(todos.size(), porEstado, Math.round(promedio * 10.0) / 10.0, lecturas, ultima);
    }
}
