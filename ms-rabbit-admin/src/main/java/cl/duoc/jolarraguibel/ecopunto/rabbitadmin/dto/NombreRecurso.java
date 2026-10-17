package cl.duoc.jolarraguibel.ecopunto.rabbitadmin.dto;

/**
 * Regla común para nombres de colas y exchanges: 3 a 100 caracteres, solo letras, números,
 * punto, guion y guion bajo, y sin el prefijo "amq." (reservado por RabbitMQ).
 */
public final class NombreRecurso {

    public static final String REGEX = "^(?!amq\\.)[A-Za-z0-9][A-Za-z0-9._-]{2,99}$";
    public static final String MENSAJE =
            "debe tener 3-100 caracteres (letras, números, '.', '-', '_') y no puede empezar con 'amq.'";

    private NombreRecurso() {
    }
}
