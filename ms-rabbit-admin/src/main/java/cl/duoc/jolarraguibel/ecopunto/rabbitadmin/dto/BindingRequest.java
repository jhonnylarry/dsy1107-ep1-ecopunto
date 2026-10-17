package cl.duoc.jolarraguibel.ecopunto.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Routing key: puede ir vacía (fanout) y admite los comodines de topic (* y #).
 */
public record BindingRequest(
        @NotBlank @Pattern(regexp = NombreRecurso.REGEX, message = NombreRecurso.MENSAJE) String exchange,
        @NotBlank @Pattern(regexp = NombreRecurso.REGEX, message = NombreRecurso.MENSAJE) String cola,
        @NotNull @Size(max = 255) @Pattern(regexp = "^[A-Za-z0-9._*#-]*$",
                message = "solo letras, números, '.', '-', '_', '*' y '#'") String routingKey) {
}
