package cl.duoc.jolarraguibel.ecopunto.monitoreo.mensajeria;

import static org.assertj.core.api.Assertions.assertThat;

import cl.duoc.jolarraguibel.ecopunto.monitoreo.dominio.ContenedorRepository;
import cl.duoc.jolarraguibel.ecopunto.monitoreo.dominio.EstadoLlenado;
import java.time.Duration;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Flujo completo contra un broker Kafka embebido: lectura válida → estado publicado y guardado;
 * JSON malformado y lectura fuera de rango → DLT, sin detener el consumidor.
 */
@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "ecopunto.kafka.reintentos.espera-inicial=100ms"
})
@EmbeddedKafka(partitions = 3, topics = {
        "ecopunto.sensores.lecturas", "ecopunto.contenedores.estado", "ecopunto.sensores.lecturas.DLT"})
@DirtiesContext
class LecturaListenerIntegracionTest {

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private ContenedorRepository repository;

    private KafkaTemplate<String, String> productorTexto() {
        Map<String, Object> props = KafkaTestUtils.producerProps(broker);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new StringSerializer()));
    }

    private Consumer<String, String> consumidor(String topic, String grupo) {
        Map<String, Object> props = KafkaTestUtils.consumerProps(broker, grupo, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(), new StringDeserializer()).createConsumer();
        broker.consumeFromAnEmbeddedTopic(consumer, topic);
        return consumer;
    }

    private static String lecturaJson(String id, String contenedor, double nivel) {
        return """
                {"lecturaId":"%s","contenedorId":"%s","puntoLimpioId":1,"material":"VIDRIO",\
                "nivelLlenado":%s,"medidoEn":"2026-11-24T10:00:00Z"}""".formatted(id, contenedor, nivel);
    }

    @Test
    void lecturasValidasEInvalidas() throws Exception {
        KafkaTemplate<String, String> productor = productorTexto();
        String topic = "ecopunto.sensores.lecturas";

        productor.send(new ProducerRecord<>(topic, "PL-9-VIDRIO", "esto no es json")).get();
        productor.send(new ProducerRecord<>(topic, "PL-9-VIDRIO", lecturaJson("x1", "PL-9-VIDRIO", 140.0))).get();
        productor.send(new ProducerRecord<>(topic, "PL-9-VIDRIO", lecturaJson("v1", "PL-9-VIDRIO", 96.0))).get();

        try (Consumer<String, String> estado = consumidor("ecopunto.contenedores.estado", "test-estado")) {
            ConsumerRecord<String, String> evento = KafkaTestUtils.getSingleRecord(estado,
                    "ecopunto.contenedores.estado", Duration.ofSeconds(30));
            assertThat(evento.key()).isEqualTo("PL-9-VIDRIO");
            assertThat(evento.value()).contains("\"estado\":\"CRITICO\"").contains("\"estadoAnterior\":\"NORMAL\"");
        }

        try (Consumer<String, String> dlt = consumidor("ecopunto.sensores.lecturas.DLT", "test-dlt")) {
            var registros = KafkaTestUtils.getRecords(dlt, Duration.ofSeconds(30), 2);
            assertThat(registros.count()).isEqualTo(2);
            registros.forEach(r -> assertThat(r.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_FQCN)).isNotNull());
            assertThat(registros).extracting(ConsumerRecord::value)
                    .anySatisfy(v -> assertThat(v).isEqualTo("esto no es json"))
                    .anySatisfy(v -> assertThat(v).contains("140.0"));
        }

        assertThat(repository.findById("PL-9-VIDRIO")).get()
                .satisfies(c -> assertThat(c.getEstado()).isEqualTo(EstadoLlenado.CRITICO));
    }
}
