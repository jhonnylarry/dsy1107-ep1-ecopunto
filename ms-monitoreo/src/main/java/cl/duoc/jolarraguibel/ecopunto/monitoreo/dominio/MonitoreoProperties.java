package cl.duoc.jolarraguibel.ecopunto.monitoreo.dominio;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Umbrales del monitoreo (ecopunto.monitoreo.*). */
@ConfigurationProperties(prefix = "ecopunto.monitoreo")
public record MonitoreoProperties(double umbralAlerta, double umbralCritico, double pesoPromedio) {

    public EstadoLlenado clasificar(double nivel) {
        if (nivel >= umbralCritico) {
            return EstadoLlenado.CRITICO;
        }
        if (nivel >= umbralAlerta) {
            return EstadoLlenado.ALERTA;
        }
        return EstadoLlenado.NORMAL;
    }
}
