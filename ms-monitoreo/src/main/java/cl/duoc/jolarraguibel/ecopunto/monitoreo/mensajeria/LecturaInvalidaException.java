package cl.duoc.jolarraguibel.ecopunto.monitoreo.mensajeria;

/** La lectura no cumple el contrato: reintentarla no la arregla, va directo al DLT. */
public class LecturaInvalidaException extends RuntimeException {

    public LecturaInvalidaException(String message) {
        super(message);
    }
}
