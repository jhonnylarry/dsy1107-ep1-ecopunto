package cl.duoc.jolarraguibel.ecopunto.tickets.web;

import cl.duoc.jolarraguibel.ecopunto.tickets.dominio.Ticket;
import cl.duoc.jolarraguibel.ecopunto.tickets.dominio.TicketService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    @GetMapping
    public List<Ticket> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ticket> buscar(@PathVariable Long id) {
        return ResponseEntity.of(service.buscar(id));
    }
}
