package cl.duoc.jolarraguibel.ecopunto.sensores.simulador;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Lee los puntos limpios del backend (endpoint público): cada material aceptado es un contenedor. */
@Component
public class PuntosLimpiosClient {

    record PuntoLimpioDto(Long id, String nombre, String materialesAceptados) {
    }

    private final RestClient restClient;
    private final SimuladorProperties props;

    public PuntosLimpiosClient(RestClient.Builder builder, SimuladorProperties props) {
        this.restClient = builder.build();
        this.props = props;
    }

    public List<Contenedor> obtenerContenedores() {
        PuntoLimpioDto[] puntos = restClient.get().uri(props.puntosUrl()).retrieve().body(PuntoLimpioDto[].class);
        List<Contenedor> contenedores = new ArrayList<>();
        if (puntos == null) {
            return contenedores;
        }
        for (PuntoLimpioDto punto : puntos) {
            materiales(punto.materialesAceptados())
                    .forEach(material -> contenedores.add(new Contenedor(punto.id(), material, 0.0)));
        }
        return contenedores;
    }

    /** "Papel, cartón, plástico" → [PAPEL, CARTON, PLASTICO]. */
    static List<String> materiales(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        return Arrays.stream(texto.split(","))
                .map(String::trim)
                .filter(m -> !m.isEmpty())
                .map(m -> Normalizer.normalize(m, Normalizer.Form.NFD).replaceAll("\\p{M}", ""))
                .map(m -> m.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_"))
                .distinct()
                .toList();
    }
}
