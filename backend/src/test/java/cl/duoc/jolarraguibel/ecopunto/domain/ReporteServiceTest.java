package cl.duoc.jolarraguibel.ecopunto.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.jolarraguibel.ecopunto.domain.evento.GenerarTicketMantencionCommand;
import cl.duoc.jolarraguibel.ecopunto.domain.evento.ReporteCreadoEvent;
import cl.duoc.jolarraguibel.ecopunto.domain.evento.ReporteEstadoCambiadoEvent;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class ReporteServiceTest {

    private final PuntoLimpioRepository puntos = mock(PuntoLimpioRepository.class);
    private final ReporteRepository reportes = mock(ReporteRepository.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final ReporteService service = new ReporteService(puntos, reportes, publisher);

    private PuntoLimpio punto;

    @BeforeEach
    void setUp() {
        punto = new PuntoLimpio("Punto Limpio Ñuñoa", "Av. Irarrázaval 3000", "Ñuñoa", "vidrio", 0.0, 0.0);
        punto.setId(1L);
    }

    private Reporte reporteEn(Reporte.Estado estado) {
        Reporte r = new Reporte(punto, "Contenedor rebalsado", "vecino@ejemplo.cl");
        r.setId(10L);
        r.setEstado(estado);
        when(reportes.findById(10L)).thenReturn(Optional.of(r));
        return r;
    }

    @Test
    void crearReportePublicaReporteCreado() {
        when(puntos.findById(1L)).thenReturn(Optional.of(punto));
        when(reportes.save(any(Reporte.class))).thenAnswer(inv -> {
            Reporte r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });

        service.crear(1L, "Contenedor rebalsado", "vecino@ejemplo.cl");

        ArgumentCaptor<Object> evento = ArgumentCaptor.forClass(Object.class);
        verify(publisher).publishEvent(evento.capture());
        assertThat(evento.getValue()).isInstanceOf(ReporteCreadoEvent.class);
        assertThat(((ReporteCreadoEvent) evento.getValue()).comuna()).isEqualTo("Ñuñoa");
    }

    @Test
    void pasarARevisionPublicaEventoYTareaDeTicket() {
        reporteEn(Reporte.Estado.PENDIENTE);

        Reporte r = service.cambiarEstado(10L, Reporte.Estado.EN_REVISION, "encargado@ecopunto.cl");

        assertThat(r.getEstado()).isEqualTo(Reporte.Estado.EN_REVISION);
        ArgumentCaptor<Object> eventos = ArgumentCaptor.forClass(Object.class);
        verify(publisher, times(2)).publishEvent(eventos.capture());
        assertThat(eventos.getAllValues()).hasExactlyElementsOfTypes(
                ReporteEstadoCambiadoEvent.class, GenerarTicketMantencionCommand.class);
    }

    @Test
    void resolverPublicaSoloElCambioDeEstado() {
        reporteEn(Reporte.Estado.EN_REVISION);

        service.cambiarEstado(10L, Reporte.Estado.RESUELTO, "encargado@ecopunto.cl");

        ArgumentCaptor<Object> eventos = ArgumentCaptor.forClass(Object.class);
        verify(publisher, times(1)).publishEvent(eventos.capture());
        assertThat(eventos.getValue()).isInstanceOf(ReporteEstadoCambiadoEvent.class);
    }

    @Test
    void transicionInvalidaNoPublicaNada() {
        reporteEn(Reporte.Estado.PENDIENTE);

        assertThatThrownBy(() -> service.cambiarEstado(10L, Reporte.Estado.RESUELTO, "encargado@ecopunto.cl"))
                .isInstanceOf(TransicionInvalidaException.class);
        verify(publisher, never()).publishEvent(any(Object.class));
    }
}
