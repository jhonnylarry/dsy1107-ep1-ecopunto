package cl.duoc.jolarraguibel.ecopunto.rabbitadmin.web;

import cl.duoc.jolarraguibel.ecopunto.rabbitadmin.servicio.ConflictoException;
import cl.duoc.jolarraguibel.ecopunto.rabbitadmin.servicio.RecursoNoEncontradoException;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.amqp.AmqpException;
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
        Map<String, String> errores = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(f -> f.getField(), f -> String.valueOf(f.getDefaultMessage()), (a, b) -> a));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Datos de entrada inválidos");
        pd.setProperty("errores", errores);
        return pd;
    }

    @ExceptionHandler({HandlerMethodValidationException.class, ConstraintViolationException.class})
    public ProblemDetail validacionParametros(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Nombre de recurso inválido");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail cuerpoIlegible(HttpMessageNotReadableException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Cuerpo JSON inválido (revise tipos: tipo de cola CLASSIC|QUORUM, tipo de exchange DIRECT|TOPIC|FANOUT|HEADERS)");
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail noEncontrado(RecursoNoEncontradoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    public ProblemDetail conflicto(ConflictoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(AmqpException.class)
    public ProblemDetail rabbit(AmqpException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "RabbitMQ rechazó la operación: " + e.getMessage());
    }
}
