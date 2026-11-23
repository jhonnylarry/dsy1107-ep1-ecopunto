package cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Map;

/**
 * @param configuracion opcional: pares como retention.ms=86400000 o cleanup.policy=compact
 */
public record CrearTopicRequest(
        @NotBlank @Pattern(regexp = NombreTopic.REGEX, message = NombreTopic.MENSAJE) String nombre,
        @NotNull @Min(value = 1, message = "debe tener al menos 1 partición") Integer particiones,
        @NotNull @Min(value = 1, message = "el factor de replicación mínimo es 1")
        @Max(value = 3, message = "el clúster tiene 3 brokers: el factor de replicación máximo es 3")
        Short replicationFactor,
        Map<String, String> configuracion) {

    public Map<String, String> configuracionOVacia() {
        return configuracion == null ? Map.of() : configuracion;
    }
}
