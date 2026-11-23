package cl.duoc.jolarraguibel.ecopunto.kafkaadmin.servicio;

/** La operación entra en conflicto con el estado actual. */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String message) {
        super(message);
    }
}
