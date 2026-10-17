package cl.duoc.jolarraguibel.ecopunto.domain;

public class TransicionInvalidaException extends RuntimeException {

    public TransicionInvalidaException(Reporte.Estado actual, Reporte.Estado nuevo) {
        super("No se puede pasar un reporte de " + actual + " a " + nuevo);
    }
}
