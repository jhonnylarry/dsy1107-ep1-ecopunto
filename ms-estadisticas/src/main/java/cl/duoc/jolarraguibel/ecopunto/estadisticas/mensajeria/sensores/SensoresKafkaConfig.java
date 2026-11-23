package cl.duoc.jolarraguibel.ecopunto.estadisticas.mensajeria.sensores;

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
 * Manejo de errores del consumidor de lecturas:
 * - error transitorio (por ejemplo la base de datos): reintentos con espera creciente;
 * - lectura inválida o JSON malformado: sin reintentos.
 * Al agotar los intentos el mensaje se descarta con un log que identifica topic, partición y offset.
 * No se publica al DLT: esa responsabilidad es de ms-monitoreo, que lee el mismo topic. Si ambos
 * grupos lo hicieran, cada mensaje defectuoso quedaría dos veces en el DLT.
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
                log.error("[DESCARTADA] {}-{}@{} key={} | motivo: {}", registro.topic(), registro.partition(),
                        registro.offset(), registro.key(), causaRaiz(error)), backOff);
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

    private static String causaRaiz(Throwable error) {
        Throwable causa = error;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }
        return causa.getClass().getSimpleName() + ": " + causa.getMessage();
    }
}
