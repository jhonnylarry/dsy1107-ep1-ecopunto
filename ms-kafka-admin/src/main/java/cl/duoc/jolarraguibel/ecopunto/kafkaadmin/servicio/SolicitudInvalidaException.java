package cl.duoc.jolarraguibel.ecopunto.kafkaadmin.servicio;

/** Kafka rechazó los parámetros. */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String message) {
        super(message);
    }
}
