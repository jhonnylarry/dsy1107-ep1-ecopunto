package cl.duoc.jolarraguibel.ecopunto.sensores.mensajeria;

import java.time.Instant;

/**
 * Contrato JSON de una lectura en ecopunto.sensores.lecturas.
 *
 * lecturaId permite al consumidor descartar duplicados; contenedorId es la key del mensaje
 * (orden por contenedor); medidoEn es el momento de la medición, no el de llegada.
 */
public record LecturaSensorEvent(
        String lecturaId,
        String contenedorId,
        Long puntoLimpioId,
        String material,
        double nivelLlenado,
        Instant medidoEn) {
}
