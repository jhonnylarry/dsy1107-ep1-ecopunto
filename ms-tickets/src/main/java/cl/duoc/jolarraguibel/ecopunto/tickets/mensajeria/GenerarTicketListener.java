package cl.duoc.jolarraguibel.ecopunto.tickets.mensajeria;

import cl.duoc.jolarraguibel.ecopunto.tickets.dominio.SolicitudTicket;
import cl.duoc.jolarraguibel.ecopunto.tickets.dominio.TicketService;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Consumidor del dominio "tickets": traduce el mensaje y delega en TicketService.
 */
@Component
public class GenerarTicketListener {

    private static final Logger log = LoggerFactory.getLogger(GenerarTicketListener.class);

    private final TicketService ticketService;
    private final ConsumidorConAck consumidor;
    private final ObjectMapper objectMapper;

    public GenerarTicketListener(TicketService ticketService, ConsumidorConAck consumidor, ObjectMapper objectMapper) {
        this.ticketService = ticketService;
        this.consumidor = consumidor;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "${ecopunto.messaging.cola}")
    public void onGenerarTicket(Message message, Channel channel) throws IOException {
        log.info("[CONSUMER] mensaje recibido cola={} routingKey={} messageId={} redelivered={}",
                message.getMessageProperties().getConsumerQueue(),
                message.getMessageProperties().getReceivedRoutingKey(),
                message.getMessageProperties().getMessageId(),
                message.getMessageProperties().isRedelivered());

        consumidor.procesar(message, channel, () -> {
            GenerarTicketMantencionCommand cmd = leer(message);
            ticketService.generar(new SolicitudTicket(cmd.messageId(), cmd.reporteId(), cmd.puntoLimpioId(),
                    cmd.puntoNombre(), cmd.direccion(), cmd.comuna(), cmd.descripcion(), cmd.solicitadoPor()));
        });
    }

    private GenerarTicketMantencionCommand leer(Message message) {
        GenerarTicketMantencionCommand cmd;
        try {
            cmd = objectMapper.readValue(message.getBody(), GenerarTicketMantencionCommand.class);
        } catch (JacksonException e) {
            throw new MensajeNoRecuperableException("JSON inválido: " + e.getOriginalMessage(), e);
        }
        if (cmd.reporteId() == null || cmd.puntoLimpioId() == null || cmd.puntoNombre() == null
                || cmd.descripcion() == null || cmd.descripcion().isBlank()) {
            throw new MensajeNoRecuperableException("faltan campos obligatorios (reporteId, puntoLimpioId, puntoNombre, descripcion)");
        }
        return cmd;
    }
}
