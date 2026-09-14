package cl.duoc.jolarraguibel.ecopunto.web;

import cl.duoc.jolarraguibel.ecopunto.domain.PuntoLimpio;
import cl.duoc.jolarraguibel.ecopunto.domain.PuntoLimpioRepository;
import cl.duoc.jolarraguibel.ecopunto.domain.Reporte;
import cl.duoc.jolarraguibel.ecopunto.domain.ReporteRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ReporteController {

    private final PuntoLimpioRepository puntoLimpioRepository;
    private final ReporteRepository reporteRepository;

    public ReporteController(
            PuntoLimpioRepository puntoLimpioRepository,
            ReporteRepository reporteRepository) {
        this.puntoLimpioRepository = puntoLimpioRepository;
        this.reporteRepository = reporteRepository;
    }

    @GetMapping("/api/puntos-limpios/{id}/reportes")
    public List<Reporte> listar(@PathVariable Long id) {
        return reporteRepository.findByPuntoLimpioId(id);
    }

    @PostMapping("/api/puntos-limpios/{id}/reportes")
    public ResponseEntity<Reporte> crear(
            @PathVariable Long id,
            @RequestBody ReporteCreateRequest request,
            JwtAuthenticationToken authentication) {

        PuntoLimpio puntoLimpio = puntoLimpioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String autorEmail = extraerEmail(authentication.getToken());

        Reporte reporte = new Reporte(puntoLimpio, request.descripcion(), autorEmail);

        return ResponseEntity.status(HttpStatus.CREATED).body(reporteRepository.save(reporte));
    }

    private String extraerEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("preferred_username");
        if (email == null) {
            email = jwt.getClaimAsString("email");
        }
        if (email == null) {
            email = jwt.getSubject();
        }
        return email;
    }

    public record ReporteCreateRequest(String descripcion) {
    }
}
