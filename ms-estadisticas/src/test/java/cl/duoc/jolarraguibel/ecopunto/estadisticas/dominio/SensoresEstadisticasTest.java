package cl.duoc.jolarraguibel.ecopunto.estadisticas.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SensoresEstadisticasTest {

    /** Repositorio en memoria para revisar los valores que quedan en cada contador. */
    private final Map<String, Contador> tabla = new HashMap<>();
    private final ContadorRepository contadores = mock(ContadorRepository.class);
    private final SensoresEstadisticas servicio = new SensoresEstadisticas(contadores, ZoneId.of("America/Santiago"));

    @BeforeEach
    void repositorioEnMemoria() {
        when(contadores.findById(any())).thenAnswer(i -> Optional.ofNullable(tabla.get(i.<String>getArgument(0))));
        when(contadores.save(any())).thenAnswer(i -> {
            Contador c = i.getArgument(0);
            tabla.put(c.getClave(), c);
            return c;
        });
        when(contadores.findByClaveStartingWith(any())).thenAnswer(i -> tabla.values().stream()
                .filter(c -> c.getClave().startsWith(i.<String>getArgument(0))).toList());
    }

    private static LecturaSensor lectura(String contenedor, long punto, String material, double nivel, Instant t) {
        return new LecturaSensor(contenedor, punto, material, nivel, t);
    }

    @Test
    void cuentaLecturasPorPuntoYMaterialYGuardaElMaximoDelDia() {
        Instant ahora = Instant.now();
        servicio.registrar(lectura("PL-1-VIDRIO", 1, "VIDRIO", 40.2, ahora.minusSeconds(10)));
        servicio.registrar(lectura("PL-1-VIDRIO", 1, "VIDRIO", 81.6, ahora.minusSeconds(5)));
        servicio.registrar(lectura("PL-1-PAPEL", 1, "PAPEL", 20.0, ahora));
        servicio.registrar(lectura("PL-2-VIDRIO", 2, "VIDRIO", 10.0, ahora));

        SensoresEstadisticas.Resumen resumen = servicio.resumen();

        assertThat(resumen.lecturasTotales()).isEqualTo(4);
        assertThat(resumen.lecturasPorPunto()).containsOnly(Map.entry("1", 3L), Map.entry("2", 1L));
        assertThat(resumen.lecturasPorMaterial()).containsEntry("VIDRIO", 3L).containsEntry("PAPEL", 1L);
        assertThat(resumen.nivelMaximoHoyPorPunto()).containsEntry("1", 82L).containsEntry("2", 10L);
    }

    @Test
    void lecturaRepetidaOAtrasadaNoSeCuenta() {
        Instant t = Instant.now();
        assertThat(servicio.registrar(lectura("PL-1-VIDRIO", 1, "VIDRIO", 50, t))).isTrue();
        assertThat(servicio.registrar(lectura("PL-1-VIDRIO", 1, "VIDRIO", 50, t))).isFalse();
        assertThat(servicio.registrar(lectura("PL-1-VIDRIO", 1, "VIDRIO", 99, t.minusSeconds(30)))).isFalse();

        SensoresEstadisticas.Resumen resumen = servicio.resumen();
        assertThat(resumen.lecturasTotales()).isEqualTo(1);
        assertThat(resumen.nivelMaximoHoyPorPunto()).containsEntry("1", 50L);
    }

    @Test
    void sinLecturasElResumenQuedaVacio() {
        when(contadores.findByClaveStartingWith(any())).thenReturn(List.of());

        SensoresEstadisticas.Resumen resumen = servicio.resumen();

        assertThat(resumen.lecturasTotales()).isZero();
        assertThat(resumen.lecturasPorPunto()).isEmpty();
        assertThat(resumen.nivelMaximoHoyPorPunto()).isEmpty();
    }
}
