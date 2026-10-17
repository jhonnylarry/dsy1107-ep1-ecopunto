package cl.duoc.jolarraguibel.ecopunto.estadisticas.dominio;

import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de estadísticas. Contabiliza cada evento una sola vez (idempotencia por eventId):
 * el registro del evento y los contadores se guardan en la misma transacción.
 */
@Service
public class EstadisticasService {

    private static final Logger log = LoggerFactory.getLogger(EstadisticasService.class);

    private final ContadorRepository contadores;
    private final EventoProcesadoRepository procesados;

    public EstadisticasService(ContadorRepository contadores, EventoProcesadoRepository procesados) {
        this.contadores = contadores;
        this.procesados = procesados;
    }

    /**
     * @param tipo último segmento de la routing key: creado, en_revision, resuelto
     */
    @Transactional
    public void registrar(String eventId, String tipo, String puntoNombre, String comuna) {
        if (procesados.existsById(eventId)) {
            log.warn("[DUPLICADO] evento {} ya contabilizado; se ignora", eventId);
            return;
        }
        procesados.save(new EventoProcesado(eventId, tipo));
        incrementar("estado:" + tipo);
        if ("creado".equals(tipo)) {
            incrementar("comuna:" + comuna);
            incrementar("punto:" + puntoNombre);
        }
        log.info("[ESTADISTICAS] evento={} tipo={} punto=\"{}\" comuna={}", eventId, tipo, puntoNombre, comuna);
    }

    public Resumen resumen() {
        Map<String, Long> porEstado = agrupar("estado:");
        return new Resumen(
                porEstado.getOrDefault("creado", 0L),
                porEstado.getOrDefault("en_revision", 0L),
                porEstado.getOrDefault("resuelto", 0L),
                agrupar("comuna:"),
                agrupar("punto:"));
    }

    private void incrementar(String clave) {
        Contador contador = contadores.findById(clave).orElseGet(() -> new Contador(clave));
        contador.incrementar();
        contadores.save(contador);
    }

    private Map<String, Long> agrupar(String prefijo) {
        Map<String, Long> resultado = new TreeMap<>();
        contadores.findByClaveStartingWith(prefijo)
                .forEach(c -> resultado.put(c.getClave().substring(prefijo.length()), c.getValor()));
        return resultado;
    }

    public record Resumen(
            long reportesCreados,
            long pasadosARevision,
            long resueltos,
            Map<String, Long> reportesPorComuna,
            Map<String, Long> reportesPorPunto) {
    }
}
