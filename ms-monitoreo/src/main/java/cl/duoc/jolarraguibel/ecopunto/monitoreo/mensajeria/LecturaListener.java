package cl.duoc.jolarraguibel.ecopunto.monitoreo.mensajeria;

import cl.duoc.jolarraguibel.ecopunto.monitoreo.dominio.MonitoreoService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor de ecopunto.sensores.lecturas (grupo ecopunto-monitoreo, 3 hilos).
 * Solo valida el contrato y delega en el dominio; los fallos los resuelve el error handler de KafkaConfig.
 */
@Component
public class LecturaListener {

    private static final Logger log = LoggerFactory.getLogger(LecturaListener.class);

    private final MonitoreoService monitoreoService;
    private final Validator validator;

    public LecturaListener(MonitoreoService monitoreoService, Validator validator) {
        this.monitoreoService = monitoreoService;
        this.validator = validator;
    }

    @KafkaListener(id = "monitoreo-lecturas", topics = "${ecopunto.kafka.topic-lecturas}")
    public void recibir(ConsumerRecord<String, LecturaSensorEvent> registro) {
        LecturaSensorEvent lectura = registro.value();
        validar(registro.key(), lectura);
        log.debug("[LECTURA] {}-{}@{} {} nivel={}", registro.topic(), registro.partition(), registro.offset(),
                lectura.contenedorId(), lectura.nivelLlenado());
        monitoreoService.procesar(lectura);
    }

    private void validar(String key, LecturaSensorEvent lectura) {
        if (lectura == null) {
            throw new LecturaInvalidaException("mensaje sin contenido");
        }
        Set<ConstraintViolation<LecturaSensorEvent>> violaciones = validator.validate(lectura);
        if (!violaciones.isEmpty()) {
            throw new LecturaInvalidaException(violaciones.stream()
                    .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; ")));
        }
        // La key decide la partición: si no coincide, el orden por contenedor no está garantizado.
        if (!lectura.contenedorId().equals(key)) {
            throw new LecturaInvalidaException("la key '" + key + "' no coincide con contenedorId '"
                    + lectura.contenedorId() + "'");
        }
    }
}
