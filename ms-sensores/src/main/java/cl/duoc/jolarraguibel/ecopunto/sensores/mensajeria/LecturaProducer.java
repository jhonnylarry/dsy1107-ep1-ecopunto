package cl.duoc.jolarraguibel.ecopunto.sensores.mensajeria;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/** Publica lecturas en ecopunto.sensores.lecturas usando contenedorId como key. */
@Component
public class LecturaProducer {

    private static final Logger log = LoggerFactory.getLogger(LecturaProducer.class);

    private final KafkaTemplate<String, LecturaSensorEvent> kafkaTemplate;
    private final TopicosProperties props;

    public LecturaProducer(KafkaTemplate<String, LecturaSensorEvent> kafkaTemplate, TopicosProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
    }

    /** Envío asíncrono para el simulador: no bloquea el ciclo y registra el resultado. */
    public CompletableFuture<SendResult<String, LecturaSensorEvent>> publicar(LecturaSensorEvent lectura) {
        return kafkaTemplate.send(props.topicLecturas(), lectura.contenedorId(), lectura)
                .whenComplete((resultado, error) -> {
                    if (error != null) {
                        log.error("[KAFKA] No se pudo publicar la lectura {} de {}: {}",
                                lectura.lecturaId(), lectura.contenedorId(), error.getMessage());
                    } else {
                        RecordMetadata meta = resultado.getRecordMetadata();
                        log.debug("[KAFKA] {} nivel={} → {}-{}@{}", lectura.contenedorId(),
                                lectura.nivelLlenado(), meta.topic(), meta.partition(), meta.offset());
                    }
                });
    }

    /** Envío que espera la confirmación del broker: lo usa la API para devolver partición y offset. */
    public LecturaPublicada publicarYConfirmar(LecturaSensorEvent lectura) {
        try {
            RecordMetadata meta = publicar(lectura).get(10, TimeUnit.SECONDS).getRecordMetadata();
            log.info("[KAFKA] Lectura manual {} nivel={} → {}-{}@{}", lectura.contenedorId(),
                    lectura.nivelLlenado(), meta.topic(), meta.partition(), meta.offset());
            return new LecturaPublicada(lectura.lecturaId(), lectura.contenedorId(), lectura.nivelLlenado(),
                    meta.topic(), meta.partition(), meta.offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PublicacionFallidaException("Publicación interrumpida", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new PublicacionFallidaException("Kafka no confirmó la lectura: " + e.getMessage(), e);
        }
    }
}
