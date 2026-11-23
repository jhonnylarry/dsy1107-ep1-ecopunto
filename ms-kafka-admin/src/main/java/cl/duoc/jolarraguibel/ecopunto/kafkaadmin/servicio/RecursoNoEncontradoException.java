package cl.duoc.jolarraguibel.ecopunto.kafkaadmin.servicio;

/** El recurso solicitado no existe. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String message) {
        super(message);
    }
}
