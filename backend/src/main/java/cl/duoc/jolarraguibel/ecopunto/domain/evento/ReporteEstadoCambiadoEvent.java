package cl.duoc.jolarraguibel.ecopunto.domain.evento;

import java.time.Instant;

/**
 * Evento de dominio: un reporte cambió de estado (EN_REVISION o RESUELTO).
 */
public record ReporteEstadoCambiadoEvent(
        String eventId,
        Instant ocurridoEn,
        Long reporteId,
        Long puntoLimpioId,
        String puntoNombre,
        String comuna,
        String estadoAnterior,
        String estadoNuevo,
        String autorEmail,
        String cambiadoPor) {
}
