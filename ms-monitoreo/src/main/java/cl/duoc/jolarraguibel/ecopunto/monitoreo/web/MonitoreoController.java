package cl.duoc.jolarraguibel.ecopunto.monitoreo.web;

import cl.duoc.jolarraguibel.ecopunto.monitoreo.dominio.Contenedor;
import cl.duoc.jolarraguibel.ecopunto.monitoreo.dominio.MonitoreoService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitoreo")
public class MonitoreoController {

    private final MonitoreoService service;

    public MonitoreoController(MonitoreoService service) {
        this.service = service;
    }

    @GetMapping("/contenedores")
    public List<Contenedor> contenedores() {
        return service.listar();
    }

    @GetMapping("/contenedores/{contenedorId}")
    public ResponseEntity<Contenedor> contenedor(@PathVariable String contenedorId) {
        return ResponseEntity.of(service.buscar(contenedorId));
    }

    /** Resultado analítico: contenedores por estado, nivel promedio y lecturas procesadas. */
    @GetMapping("/resumen")
    public MonitoreoService.Resumen resumen() {
        return service.resumen();
    }
}
