package cl.duoc.jolarraguibel.ecopunto.estadisticas.mensajeria.sensores;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Topic de lecturas y política de reintentos del consumidor Kafka (ecopunto.sensores.*). */
@ConfigurationProperties(prefix = "ecopunto.sensores")
public record SensoresProperties(String topicLecturas, Reintentos reintentos) {

    public record Reintentos(int maxIntentos, Duration esperaInicial, double multiplicador) {
    }
}
