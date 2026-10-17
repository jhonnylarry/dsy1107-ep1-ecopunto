package cl.duoc.jolarraguibel.ecopunto.notificaciones.mensajeria;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Topología de mensajería de ms-notificaciones, leída desde application.yml (ecopunto.messaging.*). */
@ConfigurationProperties(prefix = "ecopunto.messaging")
public record MessagingProperties(
        Exchanges exchanges,
        String cola,
        String colaDlq,
        RoutingKeys routingKeys,
        Reintentos reintentos) {

    public record Exchanges(String eventos, String deadLetter) {
    }

    public record RoutingKeys(String reporteCreado, String reporteResuelto) {
    }

    public record Reintentos(int maxIntentos, Duration esperaInicial) {
    }
}
