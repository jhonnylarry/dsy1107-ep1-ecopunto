package cl.duoc.jolarraguibel.ecopunto.sensores.mensajeria;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Topics de Kafka de ms-sensores, leídos desde application.yml (ecopunto.kafka.*). */
@ConfigurationProperties(prefix = "ecopunto.kafka")
public record TopicosProperties(String topicLecturas) {
}
