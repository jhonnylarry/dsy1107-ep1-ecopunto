package cl.duoc.jolarraguibel.ecopunto.notificaciones.mensajeria.sensores;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cl.duoc.jolarraguibel.ecopunto.notificaciones.dominio.NotificacionService;
import jakarta.validation.Validation;
import java.time.Instant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

class EstadoContenedoresListenerTest {

    private final NotificacionService service = mock(NotificacionService.class);
    private final EstadoContenedoresListener listener = new EstadoContenedoresListener(service,
            Validation.buildDefaultValidatorFactory().getValidator());
    private static final Instant T = Instant.parse("2026-11-24T10:00:00Z");

    private static ConsumerRecord<String, EstadoContenedorEvent> registro(EstadoLlenado anterior, EstadoLlenado estado) {
        EstadoContenedorEvent evento = new EstadoContenedorEvent("PL-1-VIDRIO", 1L, "VIDRIO", 95, 91, anterior, estado, T);
        return new ConsumerRecord<>("ecopunto.contenedores.estado", 0, 0, "PL-1-VIDRIO", evento);
    }

    @Test
    void entrarACriticoAvisaALosEncargados() {
        listener.recibir(registro(EstadoLlenado.ALERTA, EstadoLlenado.CRITICO));

        verify(service).avisarContenedorCritico(eq("estado:PL-1-VIDRIO:" + T.toEpochMilli()), eq("PL-1-VIDRIO"),
                eq(1L), eq("VIDRIO"), eq(95.0), eq(91.0));
    }

    @Test
    void otrasTransicionesNoAvisan() {
        listener.recibir(registro(EstadoLlenado.NORMAL, EstadoLlenado.ALERTA));
        listener.recibir(registro(EstadoLlenado.CRITICO, EstadoLlenado.NORMAL));

        verify(service, never()).avisarContenedorCritico(anyString(), anyString(), anyLong(), anyString(),
                anyDouble(), anyDouble());
    }

    @Test
    void eventoIncompletoEsInvalido() {
        EstadoContenedorEvent sinEstado = new EstadoContenedorEvent("PL-1-VIDRIO", 1L, "VIDRIO", 95, 91, null, null, T);

        assertThatThrownBy(() -> listener.recibir(
                new ConsumerRecord<>("ecopunto.contenedores.estado", 0, 0, "PL-1-VIDRIO", sinEstado)))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("estado es obligatorio");
    }
}
