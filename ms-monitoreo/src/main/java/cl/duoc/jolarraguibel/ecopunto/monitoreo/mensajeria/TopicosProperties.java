package cl.duoc.jolarraguibel.ecopunto.monitoreo.mensajeria;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Topics y política de reintentos de ms-monitoreo, leídos desde application.yml (ecopunto.kafka.*). */
@ConfigurationProperties(prefix = "ecopunto.kafka")
public record TopicosProperties(
        String topicLecturas,
        String topicEstado,
        String topicDlt,
        Reintentos reintentos) {

    public record Reintentos(int maxIntentos, Duration esperaInicial, double multiplicador) {
    }
}
