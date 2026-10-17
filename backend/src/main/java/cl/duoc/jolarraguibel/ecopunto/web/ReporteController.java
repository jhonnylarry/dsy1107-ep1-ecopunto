package cl.duoc.jolarraguibel.ecopunto.web;

import cl.duoc.jolarraguibel.ecopunto.domain.Reporte;
import cl.duoc.jolarraguibel.ecopunto.domain.ReporteService;
import cl.duoc.jolarraguibel.ecopunto.domain.TransicionInvalidaException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/api/puntos-limpios/{id}/reportes")
    public List<Reporte> listar(@PathVariable Long id) {
        return reporteService.listarPorPunto(id);
    }

    @PostMapping("/api/puntos-limpios/{id}/reportes")
    public ResponseEntity<Reporte> crear(
            @PathVariable Long id,
            @Valid @RequestBody ReporteCreateRequest request,
            JwtAuthenticationToken authentication) {

        Reporte reporte = reporteService.crear(id, request.descripcion(), extraerEmail(authentication.getToken()));
        return ResponseEntity.status(HttpStatus.CREATED).body(reporte);
    }

    /** El encargado avanza el reporte: PENDIENTE → EN_REVISION → RESUELTO. */
    @PatchMapping("/api/reportes/{id}/estado")
    public Reporte cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambioEstadoRequest request,
            JwtAuthenticationToken authentication) {

        return reporteService.cambiarEstado(id, request.estado(), extraerEmail(authentication.getToken()));
    }

    @ExceptionHandler(TransicionInvalidaException.class)
    public ProblemDetail transicionInvalida(TransicionInvalidaException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
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

    public record ReporteCreateRequest(@NotBlank @Size(max = 500) String descripcion) {
    }

    public record CambioEstadoRequest(@NotNull Reporte.Estado estado) {
    }
}
