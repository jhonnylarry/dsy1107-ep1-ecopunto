package cl.duoc.jolarraguibel.ecopunto.mensajeria;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de exchanges y routing keys, leídos desde application.yml
 * (ecopunto.messaging.*). Ningún nombre queda escrito en el código de negocio.
 */
@ConfigurationProperties(prefix = "ecopunto.messaging")
public record MessagingProperties(Exchanges exchanges, RoutingKeys routingKeys) {

    public record Exchanges(String eventos, String tareas) {
    }

    public record RoutingKeys(
            String reporteCreado,
            String reporteEnRevision,
            String reporteResuelto,
            String ticketMantencion) {
    }
}
