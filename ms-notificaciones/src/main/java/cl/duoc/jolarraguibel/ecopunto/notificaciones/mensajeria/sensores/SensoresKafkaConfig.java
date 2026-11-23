package cl.duoc.jolarraguibel.ecopunto.notificaciones.mensajeria.sensores;

import cl.duoc.jolarraguibel.ecopunto.notificaciones.dominio.DestinatarioInvalidoException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Manejo de errores del consumidor de estados:
 * - error transitorio (base de datos): reintentos con espera creciente;
 * - evento inválido, JSON malformado o destinatario inválido: sin reintentos.
 * Al final el mensaje se descarta con log (topic, partición, offset y motivo); el topic de estados
 * es compactado y el próximo cambio del mismo contenedor vuelve a llegar.
 */
@Configuration
@EnableConfigurationProperties(SensoresProperties.class)
public class SensoresKafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(SensoresKafkaConfig.class);

    @Bean
    DefaultErrorHandler errorHandler(SensoresProperties props) {
        SensoresProperties.Reintentos reintentos = props.reintentos();
        ExponentialBackOff backOff = new ExponentialBackOff(reintentos.esperaInicial().toMillis(),
                reintentos.multiplicador());
        backOff.setMaxAttempts(reintentos.maxIntentos() - 1);

        DefaultErrorHandler handler = new DefaultErrorHandler((registro, error) ->
                log.error("[DESCARTADO] {}-{}@{} key={} | motivo: {}", registro.topic(), registro.partition(),
                        registro.offset(), registro.key(), causaRaiz(error)), backOff);
        handler.addNotRetryableExceptions(EstadoInvalidoException.class, DestinatarioInvalidoException.class);
        handler.setRetryListeners(new RetryListener() {
            @Override
            public void failedDelivery(ConsumerRecord<?, ?> registro, Exception error, int intento) {
                log.warn("[RETRY] {}-{}@{} key={} intento {}/{} | {}", registro.topic(), registro.partition(),
                        registro.offset(), registro.key(), intento, reintentos.maxIntentos(), causaRaiz(error));
            }
        });
        return handler;
    }

    private static String causaRaiz(Throwable error) {
        Throwable causa = error;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }
        return causa.getClass().getSimpleName() + ": " + causa.getMessage();
    }
}
