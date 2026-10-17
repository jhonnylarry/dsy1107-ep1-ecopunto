package cl.duoc.jolarraguibel.ecopunto.rabbitadmin.servicio;

import cl.duoc.jolarraguibel.ecopunto.rabbitadmin.dto.BindingInfo;
import cl.duoc.jolarraguibel.ecopunto.rabbitadmin.dto.BindingRequest;
import cl.duoc.jolarraguibel.ecopunto.rabbitadmin.dto.ColaInfo;
import cl.duoc.jolarraguibel.ecopunto.rabbitadmin.dto.CrearColaRequest;
import cl.duoc.jolarraguibel.ecopunto.rabbitadmin.dto.CrearExchangeRequest;
import cl.duoc.jolarraguibel.ecopunto.rabbitadmin.dto.ExchangeInfo;
import cl.duoc.jolarraguibel.ecopunto.rabbitadmin.dto.TipoCola;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

/**
 * Toda la administración de RabbitMQ pasa por este servicio. El controlador solo llama
 * métodos de alto nivel y no conoce AmqpAdmin ni la API de Management.
 */
@Service
@EnableConfigurationProperties(AdminProperties.class)
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);

    private final AmqpAdmin amqpAdmin;
    private final RabbitManagementClient management;
    private final AdminProperties props;

    public RabbitAdminService(AmqpAdmin amqpAdmin, RabbitManagementClient management, AdminProperties props) {
        this.amqpAdmin = amqpAdmin;
        this.management = management;
        this.props = props;
    }

    // ---------- Colas ----------

    public List<ColaInfo> listarColas() {
        return management.colas().stream().map(RabbitAdminService::aColaInfo).toList();
    }

    public ColaInfo obtenerCola(String nombre) {
        return management.colas().stream()
                .filter(c -> nombre.equals(c.get("name")))
                .findFirst()
                .map(RabbitAdminService::aColaInfo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la cola " + nombre));
    }

    public ColaInfo crearCola(CrearColaRequest req, String usuario) {
        if (existeCola(req.nombre())) {
            throw new ConflictoException("La cola " + req.nombre() + " ya existe");
        }
        QueueBuilder builder = QueueBuilder.durable(req.nombre());
        if (req.tipo() == TipoCola.QUORUM) {
            builder.quorum();
        }
        if (req.crearDlq()) {
            String dlq = req.nombre() + ".dlq";
            if (existeCola(dlq)) {
                throw new ConflictoException("La cola " + dlq + " ya existe");
            }
            amqpAdmin.declareExchange(ExchangeBuilder.directExchange(props.deadLetterExchange()).durable(true).build());
            QueueBuilder dlqBuilder = QueueBuilder.durable(dlq);
            if (req.tipo() == TipoCola.QUORUM) {
                dlqBuilder.quorum();
            }
            amqpAdmin.declareQueue(dlqBuilder.build());
            amqpAdmin.declareBinding(new Binding(dlq, Binding.DestinationType.QUEUE,
                    props.deadLetterExchange(), dlq, null));
            builder.deadLetterExchange(props.deadLetterExchange()).deadLetterRoutingKey(dlq);
        }
        amqpAdmin.declareQueue(builder.build());
        log.info("[ADMIN] {} creó la cola {} (tipo={}, conDlq={})", usuario, req.nombre(), req.tipo(), req.crearDlq());
        return obtenerCola(req.nombre());
    }

    /**
     * Por seguridad solo elimina colas vacías y sin consumidores, salvo que se fuerce.
     */
    public void eliminarCola(String nombre, boolean forzar, String usuario) {
        QueueInformation info = Optional.ofNullable(amqpAdmin.getQueueInfo(nombre))
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la cola " + nombre));
        if (!forzar && (info.getMessageCount() > 0 || info.getConsumerCount() > 0)) {
            throw new ConflictoException("La cola " + nombre + " tiene " + info.getMessageCount()
                    + " mensajes y " + info.getConsumerCount() + " consumidores; use forzar=true para eliminarla igual");
        }
        amqpAdmin.deleteQueue(nombre);
        log.info("[ADMIN] {} eliminó la cola {} (forzar={})", usuario, nombre, forzar);
    }

    // ---------- Exchanges ----------

    public List<ExchangeInfo> listarExchanges() {
        return management.exchanges().stream()
                .filter(e -> !((String) e.get("name")).isEmpty())
                .map(e -> new ExchangeInfo((String) e.get("name"), (String) e.get("type"),
                        Boolean.TRUE.equals(e.get("durable"))))
                .toList();
    }

    public ExchangeInfo crearExchange(CrearExchangeRequest req, String usuario) {
        if (existeExchange(req.nombre())) {
            throw new ConflictoException("El exchange " + req.nombre() + " ya existe");
        }
        Exchange exchange = new ExchangeBuilder(req.nombre(), req.tipo().name().toLowerCase(Locale.ROOT))
                .durable(true)
                .build();
        amqpAdmin.declareExchange(exchange);
        log.info("[ADMIN] {} creó el exchange {} (tipo={})", usuario, req.nombre(), req.tipo());
        return new ExchangeInfo(exchange.getName(), exchange.getType(), true);
    }

    public void eliminarExchange(String nombre, String usuario) {
        if (!existeExchange(nombre)) {
            throw new RecursoNoEncontradoException("No existe el exchange " + nombre);
        }
        amqpAdmin.deleteExchange(nombre);
        log.info("[ADMIN] {} eliminó el exchange {}", usuario, nombre);
    }

    // ---------- Bindings ----------

    public List<BindingInfo> listarBindings() {
        return management.bindings().stream()
                .filter(b -> !((String) b.get("source")).isEmpty() && "queue".equals(b.get("destination_type")))
                .map(b -> new BindingInfo((String) b.get("source"), (String) b.get("destination"),
                        (String) b.get("routing_key")))
                .toList();
    }

    public BindingInfo crearBinding(BindingRequest req, String usuario) {
        validarExtremos(req);
        if (listarBindings().contains(new BindingInfo(req.exchange(), req.cola(), req.routingKey()))) {
            throw new ConflictoException("El binding ya existe");
        }
        amqpAdmin.declareBinding(aBinding(req));
        log.info("[ADMIN] {} creó el binding {} --{}--> {}", usuario, req.exchange(), req.routingKey(), req.cola());
        return new BindingInfo(req.exchange(), req.cola(), req.routingKey());
    }

    public void eliminarBinding(BindingRequest req, String usuario) {
        if (!listarBindings().contains(new BindingInfo(req.exchange(), req.cola(), req.routingKey()))) {
            throw new RecursoNoEncontradoException("No existe el binding " + req.exchange() + " --"
                    + req.routingKey() + "--> " + req.cola());
        }
        amqpAdmin.removeBinding(aBinding(req));
        log.info("[ADMIN] {} eliminó el binding {} --{}--> {}", usuario, req.exchange(), req.routingKey(), req.cola());
    }

    // ---------- Apoyo ----------

    private void validarExtremos(BindingRequest req) {
        if (!existeExchange(req.exchange())) {
            throw new RecursoNoEncontradoException("No existe el exchange " + req.exchange());
        }
        if (!existeCola(req.cola())) {
            throw new RecursoNoEncontradoException("No existe la cola " + req.cola());
        }
    }

    private boolean existeCola(String nombre) {
        return amqpAdmin.getQueueInfo(nombre) != null;
    }

    private boolean existeExchange(String nombre) {
        return management.exchanges().stream().anyMatch(e -> nombre.equals(e.get("name")));
    }

    private static Binding aBinding(BindingRequest req) {
        return new Binding(req.cola(), Binding.DestinationType.QUEUE, req.exchange(), req.routingKey(), null);
    }

    @SuppressWarnings("unchecked")
    private static ColaInfo aColaInfo(Map<String, Object> c) {
        return new ColaInfo((String) c.get("name"), (String) c.get("type"), Boolean.TRUE.equals(c.get("durable")),
                numero(c.get("messages")), numero(c.get("consumers")),
                (Map<String, Object>) c.getOrDefault("arguments", Map.of()));
    }

    private static long numero(Object valor) {
        return valor instanceof Number n ? n.longValue() : 0L;
    }
}
