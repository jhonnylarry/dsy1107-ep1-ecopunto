package cl.duoc.jolarraguibel.ecopunto.sensores.simulador;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ContenedorTest {

    @Test
    void idSeFormaConPuntoYMaterial() {
        assertThat(new Contenedor(3L, "VIDRIO", 0).getContenedorId()).isEqualTo("PL-3-VIDRIO");
    }

    @Test
    void elNivelNoSuperaCien() {
        Contenedor c = new Contenedor(1L, "PAPEL", 98.0);
        assertThat(c.avanzar(5.0, 10.0)).isEqualTo(100.0);
    }

    @Test
    void tresCiclosLlenoLoVacia() {
        Contenedor c = new Contenedor(1L, "PAPEL", 100.0);
        c.avanzar(1.0, 8.0);
        c.avanzar(1.0, 8.0);
        assertThat(c.avanzar(1.0, 8.0)).isEqualTo(8.0);
    }

    @Test
    void materialesSeNormalizan() {
        assertThat(PuntosLimpiosClient.materiales("Papel, cartón, plástico, vidrio"))
                .containsExactly("PAPEL", "CARTON", "PLASTICO", "VIDRIO");
        assertThat(PuntosLimpiosClient.materiales(null)).isEmpty();
    }
}
