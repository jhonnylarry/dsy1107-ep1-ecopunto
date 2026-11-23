package cl.duoc.jolarraguibel.ecopunto.kafkaadmin.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto.CrearTopicRequest;
import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto.ModificarConfigRequest;
import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto.Respuestas.TopicDetalle;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

/** Ciclo de vida completo de un topic contra un clúster embebido de 3 brokers. */
@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(count = 3, topics = "ecopunto.sensores.lecturas")
@DirtiesContext
class KafkaAdminServiceIntegracionTest {

    @Autowired
    private KafkaAdminService service;

    @Test
    void clusterMuestraLosTresBrokers() {
        assertThat(service.cluster().brokers()).hasSize(3);
    }

    @Test
    void crearModificarAumentarYEliminarUnTopic() {
        TopicDetalle creado = service.crearTopic(new CrearTopicRequest("ecopunto.auditoria", 3, (short) 3,
                Map.of("retention.ms", "86400000", "min.insync.replicas", "2")), "test");
        assertThat(creado.particiones()).isEqualTo(3);
        assertThat(creado.replicationFactor()).isEqualTo(3);
        assertThat(creado.detalleParticiones()).allSatisfy(p -> assertThat(p.replicas()).hasSize(3));
        assertThat(creado.configuracion()).containsEntry("retention.ms", "86400000");

        TopicDetalle modificado = service.modificarConfiguracion("ecopunto.auditoria",
                new ModificarConfigRequest(Map.of("cleanup.policy", "compact"), List.of("retention.ms")), "test");
        assertThat(modificado.configuracion()).containsEntry("cleanup.policy", "compact")
                .doesNotContainEntry("retention.ms", "86400000");

        assertThat(service.aumentarParticiones("ecopunto.auditoria", 6, "test").particiones()).isEqualTo(6);

        service.eliminarTopic("ecopunto.auditoria", "test");
        assertThat(service.listarTopics()).noneMatch(t -> t.nombre().equals("ecopunto.auditoria"));
    }

    @Test
    void topicDuplicadoEsConflicto() {
        assertThatThrownBy(() -> service.crearTopic(
                new CrearTopicRequest("ecopunto.sensores.lecturas", 3, (short) 3, null), "test"))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void topicDelSistemaNoSePuedeEliminar() {
        assertThatThrownBy(() -> service.eliminarTopic("ecopunto.sensores.lecturas", "test"))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void configuracionNoPermitidaSeRechaza() {
        assertThatThrownBy(() -> service.crearTopic(new CrearTopicRequest("ecopunto.otro", 1, (short) 1,
                Map.of("unclean.leader.election.enable", "true")), "test"))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void reducirParticionesSeRechaza() {
        assertThatThrownBy(() -> service.aumentarParticiones("ecopunto.sensores.lecturas", 1, "test"))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void topicInexistenteEsNoEncontrado() {
        assertThatThrownBy(() -> service.obtenerTopic("no.existe"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
