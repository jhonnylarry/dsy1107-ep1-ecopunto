package cl.duoc.jolarraguibel.ecopunto.monitoreo.mensajeria;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publica los cambios de estado en ecopunto.contenedores.estado (key = contenedorId, topic compactado). */
@Component
public class EstadoPublisher {

    private static final Logger log = LoggerFactory.getLogger(EstadoPublisher.class);

    private final KafkaTemplate<String, EstadoContenedorEvent> kafkaTemplate;
    private final TopicosProperties props;

    public EstadoPublisher(KafkaTemplate<String, EstadoContenedorEvent> kafkaTemplate, TopicosProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
    }

    /**
     * Espera la confirmación del broker: si Kafka no la da, la excepción sube al listener,
     * la lectura no se marca como procesada y el error handler la reintenta.
     */
    public void publicar(EstadoContenedorEvent evento) {
        try {
            RecordMetadata meta = kafkaTemplate.send(props.topicEstado(), evento.contenedorId(), evento)
                    .get(10, TimeUnit.SECONDS).getRecordMetadata();
            log.info("[ESTADO] {} {} → {} (nivel={}) publicado en {}-{}@{}", evento.contenedorId(),
                    evento.estadoAnterior(), evento.estado(), evento.nivelActual(),
                    meta.topic(), meta.partition(), meta.offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Publicación de estado interrumpida", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Kafka no confirmó el cambio de estado de " + evento.contenedorId(), e);
        }
    }
}
