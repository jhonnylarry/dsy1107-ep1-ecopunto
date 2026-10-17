package cl.duoc.jolarraguibel.ecopunto.notificaciones.mensajeria;

/** Error que no se arregla reintentando (mensaje mal formado o datos inválidos): va directo a la DLQ. */
public class MensajeNoRecuperableException extends RuntimeException {

    public MensajeNoRecuperableException(String mensaje) {
        super(mensaje);
    }

    public MensajeNoRecuperableException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
