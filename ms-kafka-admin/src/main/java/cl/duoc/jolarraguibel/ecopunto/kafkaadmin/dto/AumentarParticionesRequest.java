package cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Kafka solo permite aumentar particiones: el total debe ser mayor al actual. */
public record AumentarParticionesRequest(
        @NotNull @Min(value = 2, message = "el total de particiones debe ser al menos 2") Integer total) {
}
