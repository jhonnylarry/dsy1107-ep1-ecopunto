package cl.duoc.jolarraguibel.ecopunto.kafkaadmin.servicio;

import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto.CrearTopicRequest;
import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto.ModificarConfigRequest;
import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto.Respuestas.BrokerInfo;
import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto.Respuestas.ClusterInfo;
import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto.Respuestas.GrupoDetalle;
import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto.Respuestas.GrupoResumen;
import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto.Respuestas.OffsetParticion;
import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto.Respuestas.ParticionInfo;
import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto.Respuestas.TopicDetalle;
import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto.Respuestas.TopicResumen;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.GroupState;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.PolicyViolationException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Lógica de administración de Kafka sobre AdminClient: clúster, topics, configuraciones y consumer groups.
 * El controller solo traduce HTTP; aquí se validan las reglas y se traducen los errores de Kafka.
 */
@Service
public class KafkaAdminService {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminService.class);

    /** Configuraciones que siempre se muestran en el detalle, aunque tengan el valor por defecto. */
    private static final Set<String> CONFIG_RELEVANTES = Set.of(
            "cleanup.policy", "retention.ms", "retention.bytes", "min.insync.replicas", "segment.ms");

    private final AdminClient admin;
    private final AdminProperties props;

    public KafkaAdminService(AdminClient admin, AdminProperties props) {
        this.admin = admin;
        this.props = props;
    }

    // ---------- Clúster ----------

    public ClusterInfo cluster() {
        DescribeClusterResult resultado = admin.describeCluster();
        String clusterId = esperar(resultado.clusterId());
        Node controller = esperar(resultado.controller());
        List<BrokerInfo> brokers = esperar(resultado.nodes()).stream()
                .sorted(Comparator.comparingInt(Node::id))
                .map(n -> new BrokerInfo(n.id(), n.host(), n.port(), controller != null && n.id() == controller.id()))
                .toList();
        return new ClusterInfo(clusterId, controller == null ? -1 : controller.id(), brokers);
    }

    // ---------- Topics ----------

    public List<TopicResumen> listarTopics() {
        Set<String> nombres = esperar(admin.listTopics().names());
        if (nombres.isEmpty()) {
            return List.of();
        }
        return esperar(admin.describeTopics(nombres).allTopicNames()).values().stream()
                .sorted(Comparator.comparing(TopicDescription::name))
                .map(t -> new TopicResumen(t.name(), t.partitions().size(), replicationFactor(t),
                        props.esProtegido(t.name())))
                .toList();
    }

    public TopicDetalle obtenerTopic(String nombre) {
        TopicDescription t = esperar(admin.describeTopics(List.of(nombre)).allTopicNames()).get(nombre);
        List<ParticionInfo> particiones = t.partitions().stream()
                .map(p -> new ParticionInfo(p.partition(), p.leader() == null ? null : p.leader().id(),
                        ids(p.replicas()), ids(p.isr())))
                .toList();
        return new TopicDetalle(t.name(), t.partitions().size(), replicationFactor(t), props.esProtegido(t.name()),
                particiones, configuracion(nombre));
    }

    public TopicDetalle crearTopic(CrearTopicRequest req, String usuario) {
        validarConfiguraciones(req.configuracionOVacia().keySet());
        validarParticiones(req.particiones());
        NewTopic topic = new NewTopic(req.nombre(), req.particiones(), req.replicationFactor())
                .configs(req.configuracionOVacia());
        esperar(admin.createTopics(List.of(topic)).all());
        log.info("[ADMIN] {} creó el topic {} (particiones={}, RF={}, config={})", usuario, req.nombre(),
                req.particiones(), req.replicationFactor(), req.configuracionOVacia());
        return esperarMetadatos(req.nombre(), true);
    }

    public TopicDetalle modificarConfiguracion(String nombre, ModificarConfigRequest req, String usuario) {
        validarConfiguraciones(req.fijarOVacio().keySet());
        validarConfiguraciones(req.eliminarOVacio());
        obtenerTopic(nombre);

        List<AlterConfigOp> operaciones = new ArrayList<>();
        req.fijarOVacio().forEach((clave, valor) ->
                operaciones.add(new AlterConfigOp(new ConfigEntry(clave, valor), AlterConfigOp.OpType.SET)));
        req.eliminarOVacio().forEach(clave ->
                operaciones.add(new AlterConfigOp(new ConfigEntry(clave, null), AlterConfigOp.OpType.DELETE)));

        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        esperar(admin.incrementalAlterConfigs(Map.of(recurso, operaciones)).all());
        log.info("[ADMIN] {} modificó la configuración de {}: fijar={} eliminar={}", usuario, nombre,
                req.fijarOVacio(), req.eliminarOVacio());
        return obtenerTopic(nombre);
    }

    public TopicDetalle aumentarParticiones(String nombre, int total, String usuario) {
        validarParticiones(total);
        int actuales = obtenerTopic(nombre).particiones();
        if (total <= actuales) {
            throw new SolicitudInvalidaException("El topic ya tiene " + actuales
                    + " particiones; Kafka solo permite aumentarlas");
        }
        esperar(admin.createPartitions(Map.of(nombre, NewPartitions.increaseTo(total))).all());
        log.info("[ADMIN] {} aumentó las particiones de {}: {} → {}", usuario, nombre, actuales, total);
        return obtenerTopic(nombre);
    }

    public void eliminarTopic(String nombre, String usuario) {
        if (props.esProtegido(nombre)) {
            throw new ConflictoException("El topic " + nombre
                    + " es parte del flujo de EcoPunto y no se puede eliminar desde la API");
        }
        esperar(admin.deleteTopics(List.of(nombre)).all());
        log.info("[ADMIN] {} eliminó el topic {}", usuario, nombre);
        esperarMetadatos(nombre, false);
    }

    // ---------- Consumer groups ----------

    public List<GrupoResumen> listarGrupos() {
        Set<String> ids = esperar(admin.listConsumerGroups().all()).stream()
                .map(g -> g.groupId())
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return List.of();
        }
        return esperar(admin.describeConsumerGroups(ids).all()).values().stream()
                .sorted(Comparator.comparing(ConsumerGroupDescription::groupId))
                .map(g -> new GrupoResumen(g.groupId(), g.groupState().name(), g.members().size()))
                .toList();
    }

    /** Offsets confirmados, offset final y lag por partición: lag = offset final − offset confirmado. */
    public GrupoDetalle obtenerGrupo(String grupo) {
        ConsumerGroupDescription descripcion = esperar(admin.describeConsumerGroups(List.of(grupo)).all()).get(grupo);
        if (descripcion.groupState() == GroupState.DEAD) {
            throw new RecursoNoEncontradoException("No existe el consumer group " + grupo);
        }
        Map<TopicPartition, OffsetAndMetadata> confirmados =
                esperar(admin.listConsumerGroupOffsets(grupo).partitionsToOffsetAndMetadata());

        Map<TopicPartition, String> consumidorPorParticion = new HashMap<>();
        for (MemberDescription m : descripcion.members()) {
            m.assignment().topicPartitions().forEach(tp -> consumidorPorParticion.put(tp, m.clientId() + " " + m.host()));
        }

        Set<TopicPartition> particiones = new java.util.HashSet<>(confirmados.keySet());
        particiones.addAll(consumidorPorParticion.keySet());
        Map<TopicPartition, OffsetSpec> pedirFinales = particiones.stream()
                .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> finales = pedirFinales.isEmpty()
                ? Map.of()
                : esperar(admin.listOffsets(pedirFinales).all());

        List<OffsetParticion> offsets = particiones.stream()
                .sorted(Comparator.comparing(TopicPartition::topic).thenComparingInt(TopicPartition::partition))
                .map(tp -> {
                    OffsetAndMetadata confirmado = confirmados.get(tp);
                    long fin = finales.containsKey(tp) ? finales.get(tp).offset() : 0L;
                    Long offset = confirmado == null ? null : confirmado.offset();
                    Long lag = offset == null ? null : Math.max(0, fin - offset);
                    return new OffsetParticion(tp.topic(), tp.partition(), offset, fin, lag,
                            consumidorPorParticion.get(tp));
                })
                .toList();
        long lagTotal = offsets.stream().map(OffsetParticion::lag).filter(java.util.Objects::nonNull)
                .mapToLong(Long::longValue).sum();
        return new GrupoDetalle(grupo, descripcion.groupState().name(), descripcion.members().size(), lagTotal,
                offsets);
    }

    // ---------- Apoyo ----------

    /**
     * El controller confirma la creación o eliminación antes de que todos los brokers reciban los
     * metadatos nuevos; se espera un momento para que la respuesta refleje el estado real.
     */
    private TopicDetalle esperarMetadatos(String nombre, boolean debeExistir) {
        for (int intento = 1; intento <= 20; intento++) {
            try {
                TopicDetalle detalle = obtenerTopic(nombre);
                if (debeExistir) {
                    return detalle;
                }
            } catch (RecursoNoEncontradoException e) {
                if (!debeExistir) {
                    return null;
                }
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (debeExistir) {
            throw new KafkaNoDisponibleException("El topic " + nombre + " se creó, pero sus metadatos aún no están disponibles");
        }
        return null;
    }

    private Map<String, String> configuracion(String nombre) {
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Config config = esperar(admin.describeConfigs(List.of(recurso)).all()).get(recurso);
        Map<String, String> resultado = new TreeMap<>();
        for (ConfigEntry entrada : config.entries()) {
            boolean propia = entrada.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG;
            if (propia || CONFIG_RELEVANTES.contains(entrada.name())) {
                resultado.put(entrada.name(), entrada.value());
            }
        }
        return resultado;
    }

    private void validarConfiguraciones(Collection<String> claves) {
        List<String> noPermitidas = claves.stream()
                .filter(c -> !props.configuracionesPermitidas().contains(c))
                .sorted()
                .toList();
        if (!noPermitidas.isEmpty()) {
            throw new SolicitudInvalidaException("Configuraciones no permitidas: " + noPermitidas
                    + ". Permitidas: " + props.configuracionesPermitidas());
        }
    }

    private void validarParticiones(int particiones) {
        if (particiones > props.maxParticiones()) {
            throw new SolicitudInvalidaException("El máximo permitido es " + props.maxParticiones() + " particiones");
        }
    }

    private static int replicationFactor(TopicDescription t) {
        return t.partitions().isEmpty() ? 0 : t.partitions().get(0).replicas().size();
    }

    private static List<Integer> ids(List<Node> nodos) {
        return nodos.stream().map(Node::id).toList();
    }

    /** Espera una operación de AdminClient y traduce los errores de Kafka a excepciones de la API. */
    private static <T> T esperar(KafkaFuture<T> futuro) {
        try {
            return futuro.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaNoDisponibleException("Operación interrumpida");
        } catch (ExecutionException e) {
            Throwable causa = e.getCause();
            if (causa instanceof UnknownTopicOrPartitionException || causa instanceof GroupIdNotFoundException) {
                throw new RecursoNoEncontradoException(causa.getMessage());
            }
            if (causa instanceof TopicExistsException) {
                throw new ConflictoException(causa.getMessage());
            }
            if (causa instanceof InvalidConfigurationException || causa instanceof InvalidPartitionsException
                    || causa instanceof InvalidReplicationFactorException || causa instanceof InvalidRequestException
                    || causa instanceof InvalidTopicException || causa instanceof PolicyViolationException) {
                throw new SolicitudInvalidaException(causa.getMessage());
            }
            if (causa instanceof TimeoutException) {
                throw new KafkaNoDisponibleException("El clúster Kafka no respondió a tiempo");
            }
            throw new KafkaNoDisponibleException("Error de Kafka: " + causa.getMessage());
        }
    }
}
