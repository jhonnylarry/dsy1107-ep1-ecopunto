package cl.duoc.jolarraguibel.ecopunto.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CrearExchangeRequest(
        @NotBlank @Pattern(regexp = NombreRecurso.REGEX, message = NombreRecurso.MENSAJE) String nombre,
        @NotNull TipoExchange tipo) {
}
