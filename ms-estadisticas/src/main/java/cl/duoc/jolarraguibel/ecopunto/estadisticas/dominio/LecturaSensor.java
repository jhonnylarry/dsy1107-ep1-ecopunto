package cl.duoc.jolarraguibel.ecopunto.estadisticas.dominio;

import java.time.Instant;

/** Lectura de un sensor de llenado, ya validada por la capa de mensajería. */
public record LecturaSensor(
        String contenedorId,
        long puntoLimpioId,
        String material,
        double nivelLlenado,
        Instant medidoEn) {
}
