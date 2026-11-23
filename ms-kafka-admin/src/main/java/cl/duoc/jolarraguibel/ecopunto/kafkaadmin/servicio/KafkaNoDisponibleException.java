package cl.duoc.jolarraguibel.ecopunto.kafkaadmin.servicio;

/** El clúster Kafka no respondió. */
public class KafkaNoDisponibleException extends RuntimeException {

    public KafkaNoDisponibleException(String message) {
        super(message);
    }
}
