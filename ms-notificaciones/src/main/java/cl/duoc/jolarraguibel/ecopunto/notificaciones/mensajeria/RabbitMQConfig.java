package cl.duoc.jolarraguibel.ecopunto.notificaciones.mensajeria;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Caso de uso "notificar cambios en reportes":
 *   ecopunto.events (topic) --reporte.creado---> notificaciones.reportes
 *   ecopunto.events (topic) --reporte.resuelto-> notificaciones.reportes
 *   notificaciones.reportes --(nack / reintentos agotados)--> ecopunto.dlx --> notificaciones.reportes.dlq
 */
@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class RabbitMQConfig {

    @Bean
    TopicExchange eventosExchange(MessagingProperties props) {
        return ExchangeBuilder.topicExchange(props.exchanges().eventos()).durable(true).build();
    }

    @Bean
    DirectExchange deadLetterExchange(MessagingProperties props) {
        return ExchangeBuilder.directExchange(props.exchanges().deadLetter()).durable(true).build();
    }

    @Bean
    Queue notificacionesQueue(MessagingProperties props) {
        return QueueBuilder.durable(props.cola())
                .quorum()
                .deadLetterExchange(props.exchanges().deadLetter())
                .deadLetterRoutingKey(props.colaDlq())
                .build();
    }

    @Bean
    Queue notificacionesDlq(MessagingProperties props) {
        return QueueBuilder.durable(props.colaDlq()).quorum().build();
    }

    @Bean
    Binding reporteCreadoBinding(Queue notificacionesQueue, TopicExchange eventosExchange, MessagingProperties props) {
        return BindingBuilder.bind(notificacionesQueue).to(eventosExchange).with(props.routingKeys().reporteCreado());
    }

    @Bean
    Binding reporteResueltoBinding(Queue notificacionesQueue, TopicExchange eventosExchange, MessagingProperties props) {
        return BindingBuilder.bind(notificacionesQueue).to(eventosExchange).with(props.routingKeys().reporteResuelto());
    }

    @Bean
    Binding notificacionesDlqBinding(Queue notificacionesDlq, DirectExchange deadLetterExchange, MessagingProperties props) {
        return BindingBuilder.bind(notificacionesDlq).to(deadLetterExchange).with(props.colaDlq());
    }
}
