package cl.duoc.jolarraguibel.ecopunto.notificaciones.mensajeria.sensores;

/** El evento de estado no cumple el contrato: reintentar no lo arregla. */
public class EstadoInvalidoException extends RuntimeException {

    public EstadoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
