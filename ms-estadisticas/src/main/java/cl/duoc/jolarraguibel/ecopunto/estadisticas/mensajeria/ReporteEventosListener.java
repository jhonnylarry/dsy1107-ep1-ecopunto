package cl.duoc.jolarraguibel.ecopunto.estadisticas.mensajeria;

import cl.duoc.jolarraguibel.ecopunto.estadisticas.dominio.EstadisticasService;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Consumidor del dominio "estadísticas": recibe cualquier evento reporte.* y lo contabiliza.
 */
@Component
public class ReporteEventosListener {

    private static final Logger log = LoggerFactory.getLogger(ReporteEventosListener.class);

    private final EstadisticasService service;
    private final ConsumidorConAck consumidor;
    private final ObjectMapper objectMapper;

    public ReporteEventosListener(EstadisticasService service, ConsumidorConAck consumidor, ObjectMapper objectMapper) {
        this.service = service;
        this.consumidor = consumidor;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "${ecopunto.messaging.cola}")
    public void onReporteEvento(Message message, Channel channel) throws IOException {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        log.info("[CONSUMER] mensaje recibido cola={} routingKey={} messageId={} redelivered={}",
                message.getMessageProperties().getConsumerQueue(), routingKey,
                message.getMessageProperties().getMessageId(), message.getMessageProperties().isRedelivered());

        consumidor.procesar(message, channel, () -> {
            JsonNode evento;
            try {
                evento = objectMapper.readTree(message.getBody());
            } catch (JacksonException e) {
                throw new MensajeNoRecuperableException("JSON inválido: " + e.getOriginalMessage(), e);
            }
            String eventId = evento.path("eventId").asString(null);
            if (eventId == null || eventId.isBlank()) {
                throw new MensajeNoRecuperableException("falta el campo obligatorio eventId");
            }
            String tipo = routingKey.substring(routingKey.lastIndexOf('.') + 1);
            service.registrar(eventId, tipo, evento.path("puntoNombre").asString("?"),
                    evento.path("comuna").asString("?"));
        });
    }
}
