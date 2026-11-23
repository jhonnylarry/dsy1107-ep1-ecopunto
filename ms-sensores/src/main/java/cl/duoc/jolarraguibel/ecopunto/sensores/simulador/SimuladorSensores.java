package cl.duoc.jolarraguibel.ecopunto.sensores.simulador;

import cl.duoc.jolarraguibel.ecopunto.sensores.mensajeria.LecturaProducer;
import cl.duoc.jolarraguibel.ecopunto.sensores.mensajeria.LecturaPublicada;
import cl.duoc.jolarraguibel.ecopunto.sensores.mensajeria.LecturaSensorEvent;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Simula un sensor de llenado por contenedor. En cada ciclo sube el nivel de cada contenedor
 * y publica una lectura; la API permite además fijar un nivel a mano para la demo.
 */
@Service
public class SimuladorSensores {

    private static final Logger log = LoggerFactory.getLogger(SimuladorSensores.class);

    private final PuntosLimpiosClient puntosClient;
    private final LecturaProducer producer;
    private final SimuladorProperties props;
    private final Map<String, Contenedor> contenedores = new LinkedHashMap<>();

    public SimuladorSensores(PuntosLimpiosClient puntosClient, LecturaProducer producer, SimuladorProperties props) {
        this.puntosClient = puntosClient;
        this.producer = producer;
        this.props = props;
    }

    @Scheduled(fixedDelayString = "${ecopunto.simulador.intervalo}", initialDelayString = "10s")
    public void ciclo() {
        if (!props.activo()) {
            return;
        }
        if (cargarContenedoresSiFaltan() == 0) {
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (Contenedor c : contenedores()) {
            double nivel = c.avanzar(random.nextDouble(0, props.aumentoMaximo()), random.nextDouble(5, 15));
            producer.publicar(lectura(c, nivel));
        }
    }

    /** Fija el nivel de un contenedor y publica la lectura esperando la confirmación de Kafka. */
    public Optional<LecturaPublicada> registrarLecturaManual(String contenedorId, double nivel) {
        cargarContenedoresSiFaltan();
        Contenedor c;
        synchronized (contenedores) {
            c = contenedores.get(contenedorId);
        }
        if (c == null) {
            return Optional.empty();
        }
        c.fijarNivel(nivel);
        return Optional.of(producer.publicarYConfirmar(lectura(c, nivel)));
    }

    public Collection<Contenedor> contenedores() {
        synchronized (contenedores) {
            return java.util.List.copyOf(contenedores.values());
        }
    }

    private int cargarContenedoresSiFaltan() {
        synchronized (contenedores) {
            if (contenedores.isEmpty()) {
                try {
                    puntosClient.obtenerContenedores().forEach(c -> contenedores.put(c.getContenedorId(), c));
                    log.info("[SIMULADOR] {} contenedores con sensor cargados desde el backend", contenedores.size());
                } catch (RuntimeException e) {
                    log.warn("[SIMULADOR] Backend no disponible ({}); se reintenta en el próximo ciclo", e.getMessage());
                }
            }
            return contenedores.size();
        }
    }

    private static LecturaSensorEvent lectura(Contenedor c, double nivel) {
        return new LecturaSensorEvent(UUID.randomUUID().toString(), c.getContenedorId(), c.getPuntoLimpioId(),
                c.getMaterial(), nivel, Instant.now());
    }
}
