package cl.duoc.jolarraguibel.ecopunto.notificaciones.mensajeria.sensores;

import cl.duoc.jolarraguibel.ecopunto.notificaciones.dominio.NotificacionService;
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
 * Consumidor de ecopunto.contenedores.estado (grupo ecopunto-notificaciones-sensores).
 * Reacciona en tiempo real: cuando un contenedor entra a CRITICO, avisa a los encargados.
 * El id del aviso es contenedor + instante del cambio, así una reentrega no duplica el correo.
 */
@Component
public class EstadoContenedoresListener {

    private static final Logger log = LoggerFactory.getLogger(EstadoContenedoresListener.class);

    private final NotificacionService service;
    private final Validator validator;

    public EstadoContenedoresListener(NotificacionService service, Validator validator) {
        this.service = service;
        this.validator = validator;
    }

    @KafkaListener(id = "notificaciones-estado", topics = "${ecopunto.sensores.topic-estado}")
    public void recibir(ConsumerRecord<String, EstadoContenedorEvent> registro) {
        EstadoContenedorEvent evento = registro.value();
        validar(evento);
        log.info("[ESTADO] {}-{}@{} {} {} → {} ({}%)", registro.topic(), registro.partition(), registro.offset(),
                evento.contenedorId(), evento.estadoAnterior(), evento.estado(), Math.round(evento.nivelActual()));
        if (!evento.entroACritico()) {
            return;
        }
        service.avisarContenedorCritico("estado:" + evento.contenedorId() + ":" + evento.actualizadoEn().toEpochMilli(),
                evento.contenedorId(), evento.puntoLimpioId(), evento.material(),
                evento.nivelActual(), evento.nivelPromedio());
    }

    private void validar(EstadoContenedorEvent evento) {
        if (evento == null) {
            throw new EstadoInvalidoException("mensaje sin contenido");
        }
        Set<ConstraintViolation<EstadoContenedorEvent>> violaciones = validator.validate(evento);
        if (!violaciones.isEmpty()) {
            throw new EstadoInvalidoException(violaciones.stream()
                    .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; ")));
        }
    }
}
