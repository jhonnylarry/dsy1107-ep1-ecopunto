package cl.duoc.jolarraguibel.ecopunto.notificaciones.mensajeria;

import cl.duoc.jolarraguibel.ecopunto.notificaciones.dominio.DestinatarioInvalidoException;
import cl.duoc.jolarraguibel.ecopunto.notificaciones.dominio.NotificacionService;
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
 * Consumidor del dominio "notificaciones": según la routing key decide qué aviso enviar.
 */
@Component
public class ReporteEventosListener {

    private static final Logger log = LoggerFactory.getLogger(ReporteEventosListener.class);

    private final NotificacionService service;
    private final ConsumidorConAck consumidor;
    private final MessagingProperties props;
    private final ObjectMapper objectMapper;

    public ReporteEventosListener(NotificacionService service, ConsumidorConAck consumidor,
            MessagingProperties props, ObjectMapper objectMapper) {
        this.service = service;
        this.consumidor = consumidor;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "${ecopunto.messaging.cola}")
    public void onReporteEvento(Message message, Channel channel) throws IOException {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        log.info("[CONSUMER] mensaje recibido cola={} routingKey={} messageId={} redelivered={}",
                message.getMessageProperties().getConsumerQueue(), routingKey,
                message.getMessageProperties().getMessageId(), message.getMessageProperties().isRedelivered());

        consumidor.procesar(message, channel, () -> {
            JsonNode evento = leer(message);
            String eventId = requerido(evento, "eventId");
            try {
                if (props.routingKeys().reporteCreado().equals(routingKey)) {
                    service.avisarReporteNuevo(eventId, evento.path("reporteId").asLong(),
                            requerido(evento, "puntoNombre"), evento.path("comuna").asString(""),
                            evento.path("descripcion").asString(""), evento.path("autorEmail").asString(""));
                } else if (props.routingKeys().reporteResuelto().equals(routingKey)) {
                    service.avisarReporteResuelto(eventId, evento.path("reporteId").asLong(),
                            requerido(evento, "puntoNombre"), evento.path("autorEmail").asString(null));
                } else {
                    throw new MensajeNoRecuperableException("routing key no soportada: " + routingKey);
                }
            } catch (DestinatarioInvalidoException e) {
                // Reintentar no cambia el destinatario: es un error de datos, va a la DLQ.
                throw new MensajeNoRecuperableException(e.getMessage(), e);
            }
        });
    }

    private JsonNode leer(Message message) {
        try {
            return objectMapper.readTree(message.getBody());
        } catch (JacksonException e) {
            throw new MensajeNoRecuperableException("JSON inválido: " + e.getOriginalMessage(), e);
        }
    }

    private static String requerido(JsonNode evento, String campo) {
        String valor = evento.path(campo).asString(null);
        if (valor == null || valor.isBlank()) {
            throw new MensajeNoRecuperableException("falta el campo obligatorio " + campo);
        }
        return valor;
    }
}
