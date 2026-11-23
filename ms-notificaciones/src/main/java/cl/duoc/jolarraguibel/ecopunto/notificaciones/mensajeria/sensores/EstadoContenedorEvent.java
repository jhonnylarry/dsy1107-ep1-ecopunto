package cl.duoc.jolarraguibel.ecopunto.notificaciones.mensajeria.sensores;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/** Contrato JSON de ecopunto.contenedores.estado (lo publica ms-monitoreo al cambiar el estado). */
public record EstadoContenedorEvent(
        @NotBlank(message = "contenedorId es obligatorio") String contenedorId,
        @NotNull(message = "puntoLimpioId es obligatorio") Long puntoLimpioId,
        @NotBlank(message = "material es obligatorio") String material,
        double nivelActual,
        double nivelPromedio,
        EstadoLlenado estadoAnterior,
        @NotNull(message = "estado es obligatorio") EstadoLlenado estado,
        @NotNull(message = "actualizadoEn es obligatorio") Instant actualizadoEn) {

    /** Solo la entrada a CRITICO genera aviso; seguir en CRITICO o bajar de nivel no. */
    public boolean entroACritico() {
        return estado == EstadoLlenado.CRITICO && estadoAnterior != EstadoLlenado.CRITICO;
    }
}
