package cl.duoc.jolarraguibel.ecopunto.mensajeria;

import cl.duoc.jolarraguibel.ecopunto.domain.evento.GenerarTicketMantencionCommand;
import cl.duoc.jolarraguibel.ecopunto.domain.evento.ReporteCreadoEvent;
import cl.duoc.jolarraguibel.ecopunto.domain.evento.ReporteEstadoCambiadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Puente entre los eventos de dominio y RabbitMQ.
 * Publica DESPUÉS del commit: si la transacción se revierte, no sale ningún mensaje.
 */
@Component
public class RabbitEventRelay {

    private static final Logger log = LoggerFactory.getLogger(RabbitEventRelay.class);

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties props;

    public RabbitEventRelay(RabbitTemplate rabbitTemplate, MessagingProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReporteCreado(ReporteCreadoEvent event) {
        publicar(props.exchanges().eventos(), props.routingKeys().reporteCreado(), event, event.eventId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEstadoCambiado(ReporteEstadoCambiadoEvent event) {
        String routingKey = switch (event.estadoNuevo()) {
            case "EN_REVISION" -> props.routingKeys().reporteEnRevision();
            case "RESUELTO" -> props.routingKeys().reporteResuelto();
            default -> throw new IllegalArgumentException("Estado sin routing key: " + event.estadoNuevo());
        };
        publicar(props.exchanges().eventos(), routingKey, event, event.eventId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGenerarTicket(GenerarTicketMantencionCommand command) {
        publicar(props.exchanges().tareas(), props.routingKeys().ticketMantencion(), command, command.messageId());
    }

    private void publicar(String exchange, String routingKey, Object payload, String messageId) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload, message -> {
                message.getMessageProperties().setMessageId(messageId);
                return message;
            }, new CorrelationData(messageId));
            log.info("[PUBLISH] {} -> exchange={} routingKey={} messageId={}",
                    payload.getClass().getSimpleName(), exchange, routingKey, messageId);
        } catch (AmqpException e) {
            // El cambio ya está guardado; se registra el fallo de publicación para reprocesarlo.
            log.error("[PUBLISH-ERROR] no se pudo publicar {} messageId={}: {}",
                    payload.getClass().getSimpleName(), messageId, e.getMessage());
        }
    }
}
