package cl.duoc.jolarraguibel.ecopunto.kafkaadmin.servicio;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Reglas de la administración de Kafka (ecopunto.kafka-admin.*). */
@ConfigurationProperties(prefix = "ecopunto.kafka-admin")
public record AdminProperties(
        List<String> topicsProtegidos,
        List<String> configuracionesPermitidas,
        int maxParticiones) {

    public boolean esProtegido(String topic) {
        return topicsProtegidos != null && topicsProtegidos.contains(topic);
    }
}
