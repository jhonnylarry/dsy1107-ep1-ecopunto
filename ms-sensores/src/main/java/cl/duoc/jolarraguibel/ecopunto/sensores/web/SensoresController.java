package cl.duoc.jolarraguibel.ecopunto.sensores.web;

import cl.duoc.jolarraguibel.ecopunto.sensores.mensajeria.LecturaPublicada;
import cl.duoc.jolarraguibel.ecopunto.sensores.simulador.Contenedor;
import cl.duoc.jolarraguibel.ecopunto.sensores.simulador.SimuladorSensores;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sensores")
public class SensoresController {

    record ContenedorResponse(String contenedorId, Long puntoLimpioId, String material, double nivel) {
    }

    private final SimuladorSensores simulador;

    public SensoresController(SimuladorSensores simulador) {
        this.simulador = simulador;
    }

    @GetMapping("/contenedores")
    public List<ContenedorResponse> contenedores() {
        return simulador.contenedores().stream()
                .map(SensoresController::aResponse)
                .toList();
    }

    /** Publica una lectura con el nivel indicado y devuelve la partición y el offset donde quedó. */
    @PostMapping("/lecturas")
    public ResponseEntity<LecturaPublicada> registrarLectura(@Valid @RequestBody LecturaManualRequest request) {
        return simulador.registrarLecturaManual(request.contenedorId(), request.nivelLlenado())
                .map(publicada -> ResponseEntity.status(HttpStatus.CREATED).body(publicada))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Simula el retiro del material: el nivel vuelve a 0 y se publica la lectura. */
    @PostMapping("/contenedores/{contenedorId}/vaciar")
    public ResponseEntity<LecturaPublicada> vaciar(@PathVariable String contenedorId) {
        return simulador.registrarLecturaManual(contenedorId, 0.0)
                .map(publicada -> ResponseEntity.status(HttpStatus.CREATED).body(publicada))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static ContenedorResponse aResponse(Contenedor c) {
        return new ContenedorResponse(c.getContenedorId(), c.getPuntoLimpioId(), c.getMaterial(), c.getNivel());
    }
}
