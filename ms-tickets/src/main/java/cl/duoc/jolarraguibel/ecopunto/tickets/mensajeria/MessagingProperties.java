package cl.duoc.jolarraguibel.ecopunto.tickets.mensajeria;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Topología de mensajería de ms-tickets, leída desde application.yml (ecopunto.messaging.*). */
@ConfigurationProperties(prefix = "ecopunto.messaging")
public record MessagingProperties(
        Exchanges exchanges,
        String cola,
        String colaDlq,
        String routingKey,
        Reintentos reintentos) {

    public record Exchanges(String tareas, String deadLetter) {
    }

    public record Reintentos(int maxIntentos, Duration esperaInicial) {
    }
}
