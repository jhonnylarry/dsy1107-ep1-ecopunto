package cl.duoc.jolarraguibel.ecopunto.estadisticas.web;

import cl.duoc.jolarraguibel.ecopunto.estadisticas.dominio.EstadisticasService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EstadisticasController {

    private final EstadisticasService service;

    public EstadisticasController(EstadisticasService service) {
        this.service = service;
    }

    @GetMapping("/api/estadisticas")
    public EstadisticasService.Resumen resumen() {
        return service.resumen();
    }
}
