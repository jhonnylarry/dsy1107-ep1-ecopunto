package cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Las reglas de los DTO impiden crear topics con nombres vacíos o parámetros imposibles. */
class ValidacionDtosTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "ab", "__interno", "con espacio", "tildé", "-empieza-con-guion"})
    void rechazaNombresInvalidos(String nombre) {
        assertThat(validator.validate(new CrearTopicRequest(nombre, 3, (short) 3, null))).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ecopunto.auditoria", "reportes-diarios", "topic_1"})
    void aceptaNombresValidos(String nombre) {
        assertThat(validator.validate(new CrearTopicRequest(nombre, 3, (short) 3, Map.of()))).isEmpty();
    }

    @Test
    void replicationFactorNoPuedeSuperarLosBrokers() {
        assertThat(validator.validate(new CrearTopicRequest("ecopunto.prueba", 3, (short) 4, null))).isNotEmpty();
    }

    @Test
    void particionesDebenSerPositivas() {
        assertThat(validator.validate(new CrearTopicRequest("ecopunto.prueba", 0, (short) 1, null))).isNotEmpty();
    }

    @Test
    void modificarConfigSinCambiosEsInvalido() {
        assertThat(validator.validate(new ModificarConfigRequest(null, null))).isNotEmpty();
        assertThat(validator.validate(new ModificarConfigRequest(Map.of(), List.of()))).isNotEmpty();
        assertThat(validator.validate(new ModificarConfigRequest(Map.of("retention.ms", "1000"), null))).isEmpty();
    }
}
