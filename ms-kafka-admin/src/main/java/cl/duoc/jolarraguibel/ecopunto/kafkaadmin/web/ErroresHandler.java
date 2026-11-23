package cl.duoc.jolarraguibel.ecopunto.kafkaadmin.web;

import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.servicio.ConflictoException;
import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.servicio.KafkaNoDisponibleException;
import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.servicio.RecursoNoEncontradoException;
import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.servicio.SolicitudInvalidaException;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/** Traduce los errores a respuestas claras (RFC 9457 ProblemDetail). */
@RestControllerAdvice
public class ErroresHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacion(MethodArgumentNotValidException e) {
        Map<String, String> errores = e.getBindingResult().getAllErrors().stream()
                .collect(Collectors.toMap(
                        error -> error instanceof org.springframework.validation.FieldError f ? f.getField() : error.getObjectName(),
                        error -> String.valueOf(error.getDefaultMessage()), (a, b) -> a));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Datos de entrada inválidos");
        pd.setProperty("errores", errores);
        return pd;
    }

    @ExceptionHandler({HandlerMethodValidationException.class, ConstraintViolationException.class})
    public ProblemDetail validacionParametros(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Nombre de topic inválido");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail cuerpoIlegible(HttpMessageNotReadableException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Cuerpo JSON inválido (particiones y replicationFactor son números; configuracion es un objeto clave/valor)");
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ProblemDetail solicitudInvalida(SolicitudInvalidaException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail noEncontrado(RecursoNoEncontradoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    public ProblemDetail conflicto(ConflictoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(KafkaNoDisponibleException.class)
    public ProblemDetail kafkaNoDisponible(KafkaNoDisponibleException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }
}
