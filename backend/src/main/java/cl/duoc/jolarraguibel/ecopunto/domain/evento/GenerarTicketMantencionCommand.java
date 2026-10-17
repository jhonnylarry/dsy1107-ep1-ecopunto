package cl.duoc.jolarraguibel.ecopunto.domain.evento;

import java.time.Instant;

/**
 * Tarea: generar el ticket de mantención de un reporte que pasó a revisión.
 * A diferencia de un evento, va dirigida a un único tipo de trabajador.
 */
public record GenerarTicketMantencionCommand(
        String messageId,
        Instant ocurridoEn,
        Long reporteId,
        Long puntoLimpioId,
        String puntoNombre,
        String direccion,
        String comuna,
        String descripcion,
        String solicitadoPor) {
}
