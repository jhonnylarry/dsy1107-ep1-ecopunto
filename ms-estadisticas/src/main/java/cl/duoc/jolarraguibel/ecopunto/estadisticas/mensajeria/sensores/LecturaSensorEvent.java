package cl.duoc.jolarraguibel.ecopunto.estadisticas.mensajeria.sensores;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Contrato JSON de ecopunto.sensores.lecturas (el mismo que publica ms-sensores).
 * Cada servicio tiene su propia copia del DTO: comparten el formato, no el código.
 */
public record LecturaSensorEvent(
        @NotBlank(message = "lecturaId es obligatorio") String lecturaId,
        @NotBlank(message = "contenedorId es obligatorio") String contenedorId,
        @NotNull(message = "puntoLimpioId es obligatorio") Long puntoLimpioId,
        @NotBlank(message = "material es obligatorio") String material,
        @NotNull(message = "nivelLlenado es obligatorio")
        @DecimalMin(value = "0.0", message = "nivelLlenado debe estar entre 0 y 100")
        @DecimalMax(value = "100.0", message = "nivelLlenado debe estar entre 0 y 100")
        Double nivelLlenado,
        @NotNull(message = "medidoEn es obligatorio") Instant medidoEn) {
}
