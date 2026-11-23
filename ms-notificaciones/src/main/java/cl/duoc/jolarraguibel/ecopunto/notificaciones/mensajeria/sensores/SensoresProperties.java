package cl.duoc.jolarraguibel.ecopunto.notificaciones.mensajeria.sensores;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Topic de estados y política de reintentos del consumidor Kafka (ecopunto.sensores.*). */
@ConfigurationProperties(prefix = "ecopunto.sensores")
public record SensoresProperties(String topicEstado, Reintentos reintentos) {

    public record Reintentos(int maxIntentos, Duration esperaInicial, double multiplicador) {
    }
}
