package cl.duoc.jolarraguibel.ecopunto.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * @param conDlq opcional (por defecto false); si es true, también se crea "<nombre>.dlq" y la cola se configura para enviarle los mensajes fallidos
 */
public record CrearColaRequest(
        @NotBlank @Pattern(regexp = NombreRecurso.REGEX, message = NombreRecurso.MENSAJE) String nombre,
        @NotNull TipoCola tipo,
        Boolean conDlq) {

    public boolean crearDlq() {
        return Boolean.TRUE.equals(conDlq);
    }
}
