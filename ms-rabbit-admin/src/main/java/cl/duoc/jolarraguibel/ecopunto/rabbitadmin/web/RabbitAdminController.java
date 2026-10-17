package cl.duoc.jolarraguibel.ecopunto.rabbitadmin.web;

import cl.duoc.jolarraguibel.ecopunto.rabbitadmin.dto.BindingInfo;
import cl.duoc.jolarraguibel.ecopunto.rabbitadmin.dto.BindingRequest;
import cl.duoc.jolarraguibel.ecopunto.rabbitadmin.dto.ColaInfo;
import cl.duoc.jolarraguibel.ecopunto.rabbitadmin.dto.CrearColaRequest;
import cl.duoc.jolarraguibel.ecopunto.rabbitadmin.dto.CrearExchangeRequest;
import cl.duoc.jolarraguibel.ecopunto.rabbitadmin.dto.ExchangeInfo;
import cl.duoc.jolarraguibel.ecopunto.rabbitadmin.dto.NombreRecurso;
import cl.duoc.jolarraguibel.ecopunto.rabbitadmin.servicio.RabbitAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST de administración de RabbitMQ (rol ADMIN). Solo traduce HTTP a llamadas del servicio.
 */
@RestController
@Validated
@RequestMapping("/api/admin")
public class RabbitAdminController {

    private final RabbitAdminService service;

    public RabbitAdminController(RabbitAdminService service) {
        this.service = service;
    }

    @GetMapping("/queues")
    public List<ColaInfo> listarColas() {
        return service.listarColas();
    }

    @GetMapping("/queues/{nombre}")
    public ColaInfo obtenerCola(@PathVariable @Pattern(regexp = NombreRecurso.REGEX) String nombre) {
        return service.obtenerCola(nombre);
    }

    @PostMapping("/queues")
    public ResponseEntity<ColaInfo> crearCola(@Valid @RequestBody CrearColaRequest req, Principal principal) {
        ColaInfo cola = service.crearCola(req, principal.getName());
        return ResponseEntity.created(URI.create("/api/admin/queues/" + cola.nombre())).body(cola);
    }

    @DeleteMapping("/queues/{nombre}")
    public ResponseEntity<Void> eliminarCola(
            @PathVariable @Pattern(regexp = NombreRecurso.REGEX, message = NombreRecurso.MENSAJE) String nombre,
            @RequestParam(defaultValue = "false") boolean forzar,
            Principal principal) {
        service.eliminarCola(nombre, forzar, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exchanges")
    public List<ExchangeInfo> listarExchanges() {
        return service.listarExchanges();
    }

    @PostMapping("/exchanges")
    public ResponseEntity<ExchangeInfo> crearExchange(@Valid @RequestBody CrearExchangeRequest req, Principal principal) {
        ExchangeInfo exchange = service.crearExchange(req, principal.getName());
        return ResponseEntity.created(URI.create("/api/admin/exchanges/" + exchange.nombre())).body(exchange);
    }

    @DeleteMapping("/exchanges/{nombre}")
    public ResponseEntity<Void> eliminarExchange(
            @PathVariable @Pattern(regexp = NombreRecurso.REGEX, message = NombreRecurso.MENSAJE) String nombre,
            Principal principal) {
        service.eliminarExchange(nombre, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bindings")
    public List<BindingInfo> listarBindings() {
        return service.listarBindings();
    }

    @PostMapping("/bindings")
    public ResponseEntity<BindingInfo> crearBinding(@Valid @RequestBody BindingRequest req, Principal principal) {
        return ResponseEntity.status(201).body(service.crearBinding(req, principal.getName()));
    }

    /** El binding se identifica por sus tres partes, enviadas en el cuerpo. */
    @DeleteMapping("/bindings")
    public ResponseEntity<Void> eliminarBinding(@Valid @RequestBody BindingRequest req, Principal principal) {
        service.eliminarBinding(req, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
