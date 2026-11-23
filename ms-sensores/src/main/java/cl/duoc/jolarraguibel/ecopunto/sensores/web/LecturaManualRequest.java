package cl.duoc.jolarraguibel.ecopunto.sensores.web;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LecturaManualRequest(
        @NotBlank(message = "contenedorId es obligatorio") String contenedorId,
        @NotNull(message = "nivelLlenado es obligatorio")
        @DecimalMin(value = "0.0", message = "nivelLlenado debe estar entre 0 y 100")
        @DecimalMax(value = "100.0", message = "nivelLlenado debe estar entre 0 y 100")
        Double nivelLlenado) {
}
