package cl.duoc.jolarraguibel.ecopunto.kafkaadmin.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto.CrearTopicRequest;
import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto.Respuestas.GrupoDetalle;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Consumer groups y lag. Va en un clúster embebido de 1 broker: los grupos crean __consumer_offsets
 * y con 3 brokers embebidos el apagado del clúster al final de la prueba tarda minutos.
 */
@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(count = 1, brokerProperties = {"offsets.topic.num.partitions=1", "offsets.topic.replication.factor=1"})
@DirtiesContext
class KafkaAdminGruposIntegracionTest {

    @Autowired
    private KafkaAdminService service;

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Test
    void grupoInexistenteEsNoEncontrado() {
        assertThatThrownBy(() -> service.obtenerGrupo("grupo-que-no-existe"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void lagDeUnGrupoEsOffsetFinalMenosOffsetConfirmado() throws Exception {
        String topic = "ecopunto.lag.prueba";
        service.crearTopic(new CrearTopicRequest(topic, 1, (short) 1, Map.of()), "test");
        KafkaTemplate<String, String> productor = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                KafkaTestUtils.producerProps(broker), new StringSerializer(), new StringSerializer()));
        for (int i = 0; i < 5; i++) {
            productor.send(topic, "k", "m" + i).get();
        }
        // El grupo confirma haber procesado 2 de los 5 mensajes y se desconecta.
        try (Consumer<String, String> consumidor = new DefaultKafkaConsumerFactory<>(
                KafkaTestUtils.consumerProps(broker, "grupo-lag", false), new StringDeserializer(),
                new StringDeserializer()).createConsumer()) {
            consumidor.commitSync(Map.of(new TopicPartition(topic, 0), new OffsetAndMetadata(2)));
        }

        GrupoDetalle grupo = service.obtenerGrupo("grupo-lag");

        assertThat(grupo.estado()).isEqualTo("EMPTY");
        assertThat(grupo.lagTotal()).isEqualTo(3);
        assertThat(grupo.offsets()).singleElement().satisfies(o -> {
            assertThat(o.offsetConfirmado()).isEqualTo(2);
            assertThat(o.offsetFinal()).isEqualTo(5);
        });
        assertThat(service.listarGrupos()).anySatisfy(g -> assertThat(g.grupo()).isEqualTo("grupo-lag"));
    }
}
