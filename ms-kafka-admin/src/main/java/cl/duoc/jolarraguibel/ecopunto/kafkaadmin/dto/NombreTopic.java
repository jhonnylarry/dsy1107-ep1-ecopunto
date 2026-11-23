package cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto;

/**
 * Regla de nombres de topics: 3 a 200 caracteres, solo letras, números, punto, guion y guion bajo
 * (los caracteres que Kafka acepta), y sin el prefijo "__" reservado para topics internos.
 */
public final class NombreTopic {

    public static final String REGEX = "^(?!__)[A-Za-z0-9][A-Za-z0-9._-]{2,199}$";
    public static final String MENSAJE =
            "debe tener 3-200 caracteres (letras, números, '.', '-', '_') y no puede empezar con '__'";

    private NombreTopic() {
    }
}
