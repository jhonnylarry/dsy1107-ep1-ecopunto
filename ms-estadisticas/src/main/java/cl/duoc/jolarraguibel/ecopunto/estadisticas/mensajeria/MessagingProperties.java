package cl.duoc.jolarraguibel.ecopunto.estadisticas.mensajeria;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Topología de mensajería de ms-estadisticas, leída desde application.yml (ecopunto.messaging.*). */
@ConfigurationProperties(prefix = "ecopunto.messaging")
public record MessagingProperties(
        Exchanges exchanges,
        String cola,
        String colaDlq,
        String bindingKey,
        Reintentos reintentos) {

    public record Exchanges(String eventos, String deadLetter) {
    }

    public record Reintentos(int maxIntentos, Duration esperaInicial) {
    }
}
