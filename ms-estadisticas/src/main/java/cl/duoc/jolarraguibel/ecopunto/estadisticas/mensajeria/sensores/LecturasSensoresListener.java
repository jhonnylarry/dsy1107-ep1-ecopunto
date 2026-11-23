package cl.duoc.jolarraguibel.ecopunto.estadisticas.mensajeria.sensores;

import cl.duoc.jolarraguibel.ecopunto.estadisticas.dominio.LecturaSensor;
import cl.duoc.jolarraguibel.ecopunto.estadisticas.dominio.SensoresEstadisticas;
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
 * Consumidor de ecopunto.sensores.lecturas en el grupo ecopunto-estadisticas-sensores.
 * Recibe todas las lecturas, igual que ms-monitoreo, porque cada grupo lleva sus propios offsets.
 * Valida el contrato, lo traduce al modelo del dominio y delega.
 */
@Component
public class LecturasSensoresListener {

    private static final Logger log = LoggerFactory.getLogger(LecturasSensoresListener.class);

    private final SensoresEstadisticas estadisticas;
    private final Validator validator;

    public LecturasSensoresListener(SensoresEstadisticas estadisticas, Validator validator) {
        this.estadisticas = estadisticas;
        this.validator = validator;
    }

    @KafkaListener(id = "estadisticas-lecturas", topics = "${ecopunto.sensores.topic-lecturas}")
    public void recibir(ConsumerRecord<String, LecturaSensorEvent> registro) {
        LecturaSensorEvent lectura = registro.value();
        validar(registro.key(), lectura);
        log.debug("[LECTURA] {}-{}@{} {}", registro.topic(), registro.partition(), registro.offset(),
                lectura.contenedorId());
        estadisticas.registrar(new LecturaSensor(lectura.contenedorId(), lectura.puntoLimpioId(),
                lectura.material(), lectura.nivelLlenado(), lectura.medidoEn()));
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
        // Los contadores asumen que un contenedor siempre llega por la misma partición.
        if (!lectura.contenedorId().equals(key)) {
            throw new LecturaInvalidaException("la key '" + key + "' no coincide con contenedorId '"
                    + lectura.contenedorId() + "'");
        }
    }
}
