package cl.duoc.jolarraguibel.ecopunto.tickets.mensajeria;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Caso de uso "generar ticket de mantención":
 *   ecopunto.tareas (direct) --ticket.mantencion--> tickets.mantencion
 *   tickets.mantencion --(nack / reintentos agotados)--> ecopunto.dlx --> tickets.mantencion.dlq
 * Colas quorum: se replican entre los nodos del clúster.
 */
@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class RabbitMQConfig {

    @Bean
    DirectExchange tareasExchange(MessagingProperties props) {
        return ExchangeBuilder.directExchange(props.exchanges().tareas()).durable(true).build();
    }

    @Bean
    DirectExchange deadLetterExchange(MessagingProperties props) {
        return ExchangeBuilder.directExchange(props.exchanges().deadLetter()).durable(true).build();
    }

    @Bean
    Queue ticketsQueue(MessagingProperties props) {
        return QueueBuilder.durable(props.cola())
                .quorum()
                .deadLetterExchange(props.exchanges().deadLetter())
                .deadLetterRoutingKey(props.colaDlq())
                .build();
    }

    @Bean
    Queue ticketsDlq(MessagingProperties props) {
        return QueueBuilder.durable(props.colaDlq()).quorum().build();
    }

    @Bean
    Binding ticketsBinding(Queue ticketsQueue, DirectExchange tareasExchange, MessagingProperties props) {
        return BindingBuilder.bind(ticketsQueue).to(tareasExchange).with(props.routingKey());
    }

    @Bean
    Binding ticketsDlqBinding(Queue ticketsDlq, DirectExchange deadLetterExchange, MessagingProperties props) {
        return BindingBuilder.bind(ticketsDlq).to(deadLetterExchange).with(props.colaDlq());
    }
}
