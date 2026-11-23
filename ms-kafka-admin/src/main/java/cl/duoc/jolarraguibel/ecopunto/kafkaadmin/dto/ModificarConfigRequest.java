package cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto;

import jakarta.validation.constraints.AssertTrue;
import java.util.List;
import java.util.Map;

/**
 * @param fijar    configuraciones a crear o cambiar, por ejemplo {"retention.ms": "86400000"}
 * @param eliminar configuraciones que vuelven al valor por defecto del broker
 */
public record ModificarConfigRequest(Map<String, String> fijar, List<String> eliminar) {

    public Map<String, String> fijarOVacio() {
        return fijar == null ? Map.of() : fijar;
    }

    public List<String> eliminarOVacio() {
        return eliminar == null ? List.of() : eliminar;
    }

    @AssertTrue(message = "debe indicar al menos una configuración en 'fijar' o 'eliminar'")
    public boolean isConCambios() {
        return !fijarOVacio().isEmpty() || !eliminarOVacio().isEmpty();
    }
}
