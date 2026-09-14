package cl.duoc.jolarraguibel.ecopunto.web;

import cl.duoc.jolarraguibel.ecopunto.domain.PuntoLimpio;
import cl.duoc.jolarraguibel.ecopunto.domain.PuntoLimpioRepository;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
public class PuntoLimpioController {

    private final PuntoLimpioRepository puntoLimpioRepository;

    public PuntoLimpioController(PuntoLimpioRepository puntoLimpioRepository) {
        this.puntoLimpioRepository = puntoLimpioRepository;
    }

    @GetMapping("/public/puntos-limpios")
    public List<PuntoLimpio> listarPublico() {
        return puntoLimpioRepository.findAll();
    }

    @GetMapping("/api/puntos-limpios")
    public List<PuntoLimpio> listar() {
        return puntoLimpioRepository.findAll();
    }

    @PutMapping("/api/puntos-limpios/{id}")
    public ResponseEntity<PuntoLimpio> actualizar(
            @PathVariable Long id,
            @RequestBody PuntoLimpioUpdateRequest request) {

        PuntoLimpio puntoLimpio = puntoLimpioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        puntoLimpio.setNombre(request.nombre());
        puntoLimpio.setDireccion(request.direccion());
        puntoLimpio.setComuna(request.comuna());
        puntoLimpio.setMaterialesAceptados(request.materialesAceptados());

        return ResponseEntity.ok(puntoLimpioRepository.save(puntoLimpio));
    }

    public record PuntoLimpioUpdateRequest(
            String nombre,
            String direccion,
            String comuna,
            String materialesAceptados) {
    }
}
