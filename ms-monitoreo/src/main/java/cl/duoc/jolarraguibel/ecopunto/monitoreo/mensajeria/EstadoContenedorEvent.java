package cl.duoc.jolarraguibel.ecopunto.monitoreo.mensajeria;

import cl.duoc.jolarraguibel.ecopunto.monitoreo.dominio.EstadoLlenado;
import java.time.Instant;

/**
 * Contrato JSON de ecopunto.contenedores.estado. Se publica solo cuando el estado cambia;
 * estadoAnterior permite al consumidor saber qué transición ocurrió sin guardar historia.
 */
public record EstadoContenedorEvent(
        String contenedorId,
        Long puntoLimpioId,
        String material,
        double nivelActual,
        double nivelPromedio,
        EstadoLlenado estadoAnterior,
        EstadoLlenado estado,
        Instant actualizadoEn) {
}
