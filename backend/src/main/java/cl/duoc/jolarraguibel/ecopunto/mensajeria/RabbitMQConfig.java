package cl.duoc.jolarraguibel.ecopunto.mensajeria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Infraestructura de mensajería del productor. Declara los dos exchanges:
 * - eventos (topic): "algo pasó", puede tener varios interesados filtrados por patrón.
 * - tareas (direct): "haz esto", va a un único tipo de trabajador.
 * Las colas las declara cada consumidor.
 */
@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class RabbitMQConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);

    @Bean
    TopicExchange eventosExchange(MessagingProperties props) {
        return ExchangeBuilder.topicExchange(props.exchanges().eventos()).durable(true).build();
    }

    @Bean
    DirectExchange tareasExchange(MessagingProperties props) {
        return ExchangeBuilder.directExchange(props.exchanges().tareas()).durable(true).build();
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    /** Registra en el log las confirmaciones del broker y los mensajes sin ruta. */
    @Bean
    RabbitTemplateCustomizer confirmacionesCustomizer() {
        return template -> {
            template.setConfirmCallback((correlation, ack, causa) -> {
                String id = correlation != null ? correlation.getId() : "?";
                if (ack) {
                    log.info("[PUBLISH-CONFIRM] broker confirmó messageId={}", id);
                } else {
                    log.error("[PUBLISH-CONFIRM] broker RECHAZÓ messageId={} causa={}", id, causa);
                }
            });
            template.setReturnsCallback(returned -> log.error(
                    "[PUBLISH-RETURN] sin cola de destino: exchange={} routingKey={} motivo={}",
                    returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
        };
    }
}
