package cl.duoc.jolarraguibel.ecopunto.web;

import cl.duoc.jolarraguibel.ecopunto.domain.PuntoLimpio;
import cl.duoc.jolarraguibel.ecopunto.domain.PuntoLimpioRepository;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping("/api/puntos-limpios")
    public ResponseEntity<PuntoLimpio> crear(@RequestBody PuntoLimpioCreateRequest request) {
        PuntoLimpio puntoLimpio = new PuntoLimpio(
                request.nombre(),
                request.direccion(),
                request.comuna(),
                request.materialesAceptados(),
                request.latitud(),
                request.longitud());

        PuntoLimpio guardado = puntoLimpioRepository.save(puntoLimpio);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
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

    @DeleteMapping("/api/puntos-limpios/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (!puntoLimpioRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        puntoLimpioRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    public record PuntoLimpioCreateRequest(
            String nombre,
            String direccion,
            String comuna,
            String materialesAceptados,
            Double latitud,
            Double longitud) {
    }

    public record PuntoLimpioUpdateRequest(
            String nombre,
            String direccion,
            String comuna,
            String materialesAceptados) {
    }
}
