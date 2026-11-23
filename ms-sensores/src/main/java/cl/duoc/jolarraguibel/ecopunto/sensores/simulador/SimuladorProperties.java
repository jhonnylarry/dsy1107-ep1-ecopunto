package cl.duoc.jolarraguibel.ecopunto.sensores.simulador;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Parámetros del simulador de sensores (ecopunto.simulador.*). */
@ConfigurationProperties(prefix = "ecopunto.simulador")
public record SimuladorProperties(
        boolean activo,
        Duration intervalo,
        String puntosUrl,
        double aumentoMaximo) {
}
