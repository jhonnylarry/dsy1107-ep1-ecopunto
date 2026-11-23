package cl.duoc.jolarraguibel.ecopunto.notificaciones.mensajeria.sensores;

import static org.assertj.core.api.Assertions.assertThat;

import cl.duoc.jolarraguibel.ecopunto.notificaciones.dominio.Notificacion;
import cl.duoc.jolarraguibel.ecopunto.notificaciones.dominio.NotificacionRepository;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Contra un broker embebido: de varios cambios de estado (más uno malformado y uno repetido)
 * solo la entrada a CRITICO genera una notificación, y una sola vez.
 */
@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        // En la prueba los mensajes se publican antes de que el grupo tenga particiones asignadas.
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "ecopunto.sensores.reintentos.espera-inicial=100ms"
})
@EmbeddedKafka(partitions = 3, topics = "ecopunto.contenedores.estado")
@DirtiesContext
class EstadoContenedoresListenerIntegracionTest {

    private static final String TOPIC = "ecopunto.contenedores.estado";

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private NotificacionRepository repository;

    private static String estado(String anterior, String actual, double nivel, String instante) {
        return """
                {"contenedorId":"PL-3-PAPEL","puntoLimpioId":3,"material":"PAPEL","nivelActual":%s,\
                "nivelPromedio":%s,"estadoAnterior":%s,"estado":"%s","actualizadoEn":"%s"}"""
                .formatted(nivel, nivel - 5, anterior == null ? "null" : "\"" + anterior + "\"", actual, instante);
    }

    @Test
    void soloLaEntradaACriticoNotifica() throws Exception {
        Map<String, Object> props = KafkaTestUtils.producerProps(broker);
        KafkaTemplate<String, String> productor = new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new StringSerializer()));
        String critico = estado("ALERTA", "CRITICO", 93, "2026-11-24T10:05:00Z");

        productor.send(new ProducerRecord<>(TOPIC, "PL-3-PAPEL", "{no es json")).get();
        productor.send(new ProducerRecord<>(TOPIC, "PL-3-PAPEL", estado("NORMAL", "DESCONOCIDO", 50, "2026-11-24T10:00:00Z"))).get();
        productor.send(new ProducerRecord<>(TOPIC, "PL-3-PAPEL", estado(null, "ALERTA", 80, "2026-11-24T10:01:00Z"))).get();
        productor.send(new ProducerRecord<>(TOPIC, "PL-3-PAPEL", critico)).get();
        productor.send(new ProducerRecord<>(TOPIC, "PL-3-PAPEL", critico)).get();
        productor.send(new ProducerRecord<>(TOPIC, "PL-3-PAPEL", estado("CRITICO", "NORMAL", 0, "2026-11-24T10:20:00Z"))).get();
        // Marca de fin: un segundo contenedor que entra a CRITICO; como va en otra key puede ir en otra partición,
        // por eso se espera a ver las dos notificaciones.
        productor.send(new ProducerRecord<>(TOPIC, "PL-4-VIDRIO", estado("ALERTA", "CRITICO", 97, "2026-11-24T10:30:00Z")
                .replace("PL-3-PAPEL", "PL-4-VIDRIO").replace("\"puntoLimpioId\":3", "\"puntoLimpioId\":4"))).get();

        List<Notificacion> enviadas = repository.findAll();
        long limite = System.currentTimeMillis() + 30_000;
        while (enviadas.size() < 2 && System.currentTimeMillis() < limite) {
            Thread.sleep(200);
            enviadas = repository.findAll();
        }
        // El aviso repetido y la vuelta a NORMAL van en la misma partición que el primero y antes del fin de PL-3.
        Thread.sleep(1000);
        enviadas = repository.findAll();

        assertThat(enviadas).hasSize(2);
        assertThat(enviadas).extracting(Notificacion::getAsunto)
                .anySatisfy(a -> assertThat(a).contains("PL-3-PAPEL").contains("93%"))
                .anySatisfy(a -> assertThat(a).contains("PL-4-VIDRIO"));
        assertThat(enviadas).extracting(Notificacion::getEventId).doesNotHaveDuplicates();
    }
}
