package cl.duoc.jolarraguibel.ecopunto.estadisticas.mensajeria.sensores;

/** La lectura no cumple el contrato: reintentar no la arregla. */
public class LecturaInvalidaException extends RuntimeException {

    public LecturaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
