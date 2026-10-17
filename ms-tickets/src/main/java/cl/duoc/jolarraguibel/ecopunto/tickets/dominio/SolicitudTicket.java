package cl.duoc.jolarraguibel.ecopunto.tickets.dominio;

/** Datos que el dominio necesita para generar un ticket (independiente del formato del mensaje). */
public record SolicitudTicket(
        String messageId,
        Long reporteId,
        Long puntoLimpioId,
        String puntoNombre,
        String direccion,
        String comuna,
        String descripcion,
        String solicitadoPor) {
}
