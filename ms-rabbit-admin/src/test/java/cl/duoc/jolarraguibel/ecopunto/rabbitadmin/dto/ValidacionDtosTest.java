package cl.duoc.jolarraguibel.ecopunto.rabbitadmin.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Las reglas de los DTO impiden crear recursos con nombres vacíos o inválidos. */
class ValidacionDtosTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "ab", "amq.reservado", "con espacio", "tildé", "-empieza-con-guion"})
    void rechazaNombresDeColaInvalidos(String nombre) {
        assertThat(validator.validate(new CrearColaRequest(nombre, TipoCola.QUORUM, false))).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"tickets.mantencion", "auditoria-reportes", "cola_1"})
    void aceptaNombresDeColaValidos(String nombre) {
        assertThat(validator.validate(new CrearColaRequest(nombre, TipoCola.CLASSIC, null))).isEmpty();
    }

    @Test
    void colaSinTipoEsInvalida() {
        assertThat(validator.validate(new CrearColaRequest("cola.valida", null, false))).isNotEmpty();
    }

    @Test
    void exchangeSinTipoEsInvalido() {
        assertThat(validator.validate(new CrearExchangeRequest("ecopunto.auditoria", null))).isNotEmpty();
    }

    @Test
    void bindingAceptaComodinesDeTopicYRechazaCaracteresRaros() {
        assertThat(validator.validate(new BindingRequest("ecopunto.events", "auditoria.reportes", "reporte.#"))).isEmpty();
        assertThat(validator.validate(new BindingRequest("ecopunto.events", "auditoria.reportes", "hola mundo!"))).isNotEmpty();
    }

    @Test
    void conDlqEsOpcional() {
        assertThat(new CrearColaRequest("cola.valida", TipoCola.QUORUM, null).crearDlq()).isFalse();
        assertThat(new CrearColaRequest("cola.valida", TipoCola.QUORUM, true).crearDlq()).isTrue();
    }
}
