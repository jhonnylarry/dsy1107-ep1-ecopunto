package cl.duoc.jolarraguibel.ecopunto.domain.evento;

import java.time.Instant;

/**
 * Evento de dominio: se registró un reporte. Lleva los datos del punto limpio
 * para que los consumidores no tengan que consultar al backend.
 */
public record ReporteCreadoEvent(
        String eventId,
        Instant ocurridoEn,
        Long reporteId,
        Long puntoLimpioId,
        String puntoNombre,
        String comuna,
        String descripcion,
        String autorEmail) {
}
