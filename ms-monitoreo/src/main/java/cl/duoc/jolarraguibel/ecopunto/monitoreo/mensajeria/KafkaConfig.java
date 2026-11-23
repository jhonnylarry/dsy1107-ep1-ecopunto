package cl.duoc.jolarraguibel.ecopunto.monitoreo.mensajeria;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Manejo de errores del consumidor de lecturas:
 * - error transitorio (base de datos o Kafka caídos): reintentos con espera creciente;
 * - lectura inválida o JSON malformado: sin reintentos;
 * - en ambos casos, al final, el mensaje original va a ecopunto.sensores.lecturas.DLT
 *   con headers que indican topic, partición, offset y excepción de origen.
 * Así un mensaje defectuoso nunca detiene la partición.
 */
@Configuration
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Bean
    DefaultErrorHandler errorHandler(ProducerFactory<?, ?> producerFactory, TopicosProperties props) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                plantillaDlt(producerFactory),
                (registro, error) -> new TopicPartition(props.topicDlt(), registro.partition())) {
            @Override
            public void accept(ConsumerRecord<?, ?> registro, org.apache.kafka.clients.consumer.Consumer<?, ?> consumer,
                    Exception error) {
                log.error("[DLT] {}-{}@{} key={} → {} | motivo: {}", registro.topic(), registro.partition(),
                        registro.offset(), registro.key(), props.topicDlt(), causaRaiz(error));
                super.accept(registro, consumer, error);
            }
        };

        TopicosProperties.Reintentos reintentos = props.reintentos();
        ExponentialBackOff backOff = new ExponentialBackOff(reintentos.esperaInicial().toMillis(),
                reintentos.multiplicador());
        backOff.setMaxAttempts(reintentos.maxIntentos() - 1);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        // Errores de contrato: reintentar no los arregla. (DeserializationException ya viene como no reintentable.)
        handler.addNotRetryableExceptions(LecturaInvalidaException.class);
        handler.setRetryListeners(new RetryListener() {
            @Override
            public void failedDelivery(ConsumerRecord<?, ?> registro, Exception error, int intento) {
                log.warn("[RETRY] {}-{}@{} key={} intento {}/{} | {}", registro.topic(), registro.partition(),
                        registro.offset(), registro.key(), intento, reintentos.maxIntentos(), causaRaiz(error));
            }
        });
        return handler;
    }

    /**
     * Plantilla propia del DLT: un JSON malformado llega como byte[] (no se pudo deserializar) y se
     * reenvía tal cual; una lectura válida en forma pero inválida en contenido se reenvía como JSON.
     * No se registra como bean para no reemplazar el KafkaTemplate que configura Spring Boot.
     */
    @SuppressWarnings("unchecked")
    private static KafkaTemplate<Object, Object> plantillaDlt(ProducerFactory<?, ?> producerFactory) {
        Map<Class<?>, Serializer<?>> porTipo = new LinkedHashMap<>();
        porTipo.put(byte[].class, new ByteArraySerializer());
        porTipo.put(Object.class, new JacksonJsonSerializer<>());
        DefaultKafkaProducerFactory<Object, Object> factory = new DefaultKafkaProducerFactory<>(
                producerFactory.getConfigurationProperties(), (Serializer<Object>) (Serializer<?>) new StringSerializer(),
                new DelegatingByTypeSerializer(porTipo, true));
        return new KafkaTemplate<>(factory);
    }

    private static String causaRaiz(Throwable error) {
        Throwable causa = error;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }
        return causa.getClass().getSimpleName() + ": " + causa.getMessage();
    }
}
