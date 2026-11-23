package cl.duoc.jolarraguibel.ecopunto.sensores.mensajeria;

/** Dónde quedó guardada una lectura en Kafka: sirve como evidencia de publicación. */
public record LecturaPublicada(
        String lecturaId,
        String contenedorId,
        double nivelLlenado,
        String topic,
        int particion,
        long offset) {
}
