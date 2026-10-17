package cl.duoc.jolarraguibel.ecopunto.estadisticas.mensajeria;

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
 * Caso de uso "estadísticas de reportes":
 *   ecopunto.events (topic) --reporte.*--> estadisticas.reportes
 *   estadisticas.reportes --(nack / reintentos agotados)--> ecopunto.dlx --> estadisticas.reportes.dlq
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
    Queue estadisticasQueue(MessagingProperties props) {
        return QueueBuilder.durable(props.cola())
                .quorum()
                .deadLetterExchange(props.exchanges().deadLetter())
                .deadLetterRoutingKey(props.colaDlq())
                .build();
    }

    @Bean
    Queue estadisticasDlq(MessagingProperties props) {
        return QueueBuilder.durable(props.colaDlq()).quorum().build();
    }

    @Bean
    Binding estadisticasBinding(Queue estadisticasQueue, TopicExchange eventosExchange, MessagingProperties props) {
        return BindingBuilder.bind(estadisticasQueue).to(eventosExchange).with(props.bindingKey());
    }

    @Bean
    Binding estadisticasDlqBinding(Queue estadisticasDlq, DirectExchange deadLetterExchange, MessagingProperties props) {
        return BindingBuilder.bind(estadisticasDlq).to(deadLetterExchange).with(props.colaDlq());
    }
}
