package cl.duoc.jolarraguibel.ecopunto.monitoreo.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.jolarraguibel.ecopunto.monitoreo.mensajeria.EstadoContenedorEvent;
import cl.duoc.jolarraguibel.ecopunto.monitoreo.mensajeria.EstadoPublisher;
import cl.duoc.jolarraguibel.ecopunto.monitoreo.mensajeria.LecturaSensorEvent;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MonitoreoServiceTest {

    private final MonitoreoProperties reglas = new MonitoreoProperties(75.0, 90.0, 0.3);
    private ContenedorRepository repository;
    private EstadoPublisher publisher;
    private MonitoreoService service;
    private Contenedor contenedor;

    @BeforeEach
    void setUp() {
        repository = mock(ContenedorRepository.class);
        publisher = mock(EstadoPublisher.class);
        service = new MonitoreoService(repository, publisher, reglas);
        contenedor = new Contenedor("PL-1-VIDRIO", 1L, "VIDRIO");
        when(repository.findById("PL-1-VIDRIO")).thenReturn(Optional.of(contenedor));
    }

    private static LecturaSensorEvent lectura(String id, double nivel, Instant medidoEn) {
        return new LecturaSensorEvent(id, "PL-1-VIDRIO", 1L, "VIDRIO", nivel, medidoEn);
    }

    @Test
    void clasificaSegunUmbrales() {
        assertThat(reglas.clasificar(74.9)).isEqualTo(EstadoLlenado.NORMAL);
        assertThat(reglas.clasificar(75.0)).isEqualTo(EstadoLlenado.ALERTA);
        assertThat(reglas.clasificar(90.0)).isEqualTo(EstadoLlenado.CRITICO);
    }

    @Test
    void sinCambioDeEstadoNoPublica() {
        service.procesar(lectura("a", 40.0, Instant.parse("2026-11-24T10:00:00Z")));

        verify(publisher, never()).publicar(any());
        verify(repository).save(contenedor);
        assertThat(contenedor.getEstado()).isEqualTo(EstadoLlenado.NORMAL);
    }

    @Test
    void cambioACriticoPublicaLaTransicion() {
        service.procesar(lectura("a", 50.0, Instant.parse("2026-11-24T10:00:00Z")));
        service.procesar(lectura("b", 95.0, Instant.parse("2026-11-24T10:00:05Z")));

        ArgumentCaptor<EstadoContenedorEvent> evento = ArgumentCaptor.forClass(EstadoContenedorEvent.class);
        verify(publisher).publicar(evento.capture());
        assertThat(evento.getValue().estadoAnterior()).isEqualTo(EstadoLlenado.NORMAL);
        assertThat(evento.getValue().estado()).isEqualTo(EstadoLlenado.CRITICO);
        // Promedio móvil: 0,3 × 95 + 0,7 × 50 = 63,5
        assertThat(evento.getValue().nivelPromedio()).isEqualTo(63.5);
    }

    @Test
    void lecturaRepetidaSeIgnora() {
        Instant t = Instant.parse("2026-11-24T10:00:00Z");
        service.procesar(lectura("a", 95.0, t));
        service.procesar(lectura("a", 95.0, t));

        assertThat(contenedor.getLecturas()).isEqualTo(1);
        verify(publisher).publicar(any());
    }

    @Test
    void lecturaAtrasadaSeIgnora() {
        service.procesar(lectura("a", 30.0, Instant.parse("2026-11-24T10:00:05Z")));
        service.procesar(lectura("b", 99.0, Instant.parse("2026-11-24T10:00:00Z")));

        assertThat(contenedor.getNivelActual()).isEqualTo(30.0);
        verify(publisher, never()).publicar(any());
    }

    @Test
    void siKafkaFallaNoSeGuardaElContenedor() {
        doThrow(new IllegalStateException("Kafka no confirmó")).when(publisher).publicar(any());

        assertThatThrownBy(() -> service.procesar(lectura("a", 95.0, Instant.parse("2026-11-24T10:00:00Z"))))
                .isInstanceOf(IllegalStateException.class);
        verify(repository, never()).save(any());
    }
}
