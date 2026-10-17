package cl.duoc.jolarraguibel.ecopunto.tickets.mensajeria;

import java.time.Instant;

/** Contrato JSON del mensaje que publica el backend en ecopunto.tareas. */
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
