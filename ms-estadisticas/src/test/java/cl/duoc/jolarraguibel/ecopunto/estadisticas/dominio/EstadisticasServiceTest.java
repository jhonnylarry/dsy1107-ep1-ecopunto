package cl.duoc.jolarraguibel.ecopunto.estadisticas.dominio;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class EstadisticasServiceTest {

    private final ContadorRepository contadores = mock(ContadorRepository.class);
    private final EventoProcesadoRepository procesados = mock(EventoProcesadoRepository.class);
    private final EstadisticasService service = new EstadisticasService(contadores, procesados);

    @Test
    void reporteCreadoIncrementaEstadoComunaYPunto() {
        when(procesados.existsById("evt-1")).thenReturn(false);
        when(contadores.findById(any())).thenReturn(Optional.empty());

        service.registrar("evt-1", "creado", "Punto Limpio Ñuñoa", "Ñuñoa");

        verify(procesados).save(any(EventoProcesado.class));
        verify(contadores, times(3)).save(any(Contador.class));
    }

    @Test
    void cambioDeEstadoSoloIncrementaElEstado() {
        when(procesados.existsById("evt-2")).thenReturn(false);
        when(contadores.findById(any())).thenReturn(Optional.empty());

        service.registrar("evt-2", "resuelto", "Punto Limpio Ñuñoa", "Ñuñoa");

        verify(contadores, times(1)).save(any(Contador.class));
    }

    @Test
    void eventoRepetidoNoSeCuentaDosVeces() {
        when(procesados.existsById("evt-1")).thenReturn(true);

        service.registrar("evt-1", "creado", "Punto Limpio Ñuñoa", "Ñuñoa");

        verify(procesados, never()).save(any());
        verify(contadores, never()).save(any());
    }
}
