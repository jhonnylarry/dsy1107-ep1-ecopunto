package cl.duoc.jolarraguibel.ecopunto.kafkaadmin.web;

import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto.AumentarParticionesRequest;
import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto.CrearTopicRequest;
import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto.ModificarConfigRequest;
import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto.NombreTopic;
import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto.Respuestas.ClusterInfo;
import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto.Respuestas.GrupoDetalle;
import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto.Respuestas.GrupoResumen;
import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto.Respuestas.TopicDetalle;
import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto.Respuestas.TopicResumen;
import cl.duoc.jolarraguibel.ecopunto.kafkaadmin.servicio.KafkaAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST de administración de Kafka (rol ADMIN). Solo traduce HTTP a llamadas del servicio.
 */
@RestController
@Validated
@RequestMapping("/api/kafka-admin")
public class KafkaAdminController {

    private final KafkaAdminService service;

    public KafkaAdminController(KafkaAdminService service) {
        this.service = service;
    }

    @GetMapping("/cluster")
    public ClusterInfo cluster() {
        return service.cluster();
    }

    @GetMapping("/topics")
    public List<TopicResumen> listarTopics() {
        return service.listarTopics();
    }

    @GetMapping("/topics/{nombre}")
    public TopicDetalle obtenerTopic(
            @PathVariable @Pattern(regexp = NombreTopic.REGEX, message = NombreTopic.MENSAJE) String nombre) {
        return service.obtenerTopic(nombre);
    }

    @PostMapping("/topics")
    public ResponseEntity<TopicDetalle> crearTopic(@Valid @RequestBody CrearTopicRequest req,
            JwtAuthenticationToken auth) {
        TopicDetalle topic = service.crearTopic(req, usuario(auth));
        return ResponseEntity.created(URI.create("/api/kafka-admin/topics/" + topic.nombre())).body(topic);
    }

    @PatchMapping("/topics/{nombre}/config")
    public TopicDetalle modificarConfiguracion(
            @PathVariable @Pattern(regexp = NombreTopic.REGEX, message = NombreTopic.MENSAJE) String nombre,
            @Valid @RequestBody ModificarConfigRequest req, JwtAuthenticationToken auth) {
        return service.modificarConfiguracion(nombre, req, usuario(auth));
    }

    @PostMapping("/topics/{nombre}/particiones")
    public TopicDetalle aumentarParticiones(
            @PathVariable @Pattern(regexp = NombreTopic.REGEX, message = NombreTopic.MENSAJE) String nombre,
            @Valid @RequestBody AumentarParticionesRequest req, JwtAuthenticationToken auth) {
        return service.aumentarParticiones(nombre, req.total(), usuario(auth));
    }

    @DeleteMapping("/topics/{nombre}")
    public ResponseEntity<Void> eliminarTopic(
            @PathVariable @Pattern(regexp = NombreTopic.REGEX, message = NombreTopic.MENSAJE) String nombre,
            JwtAuthenticationToken auth) {
        service.eliminarTopic(nombre, usuario(auth));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/grupos")
    public List<GrupoResumen> listarGrupos() {
        return service.listarGrupos();
    }

    @GetMapping("/grupos/{grupo}")
    public GrupoDetalle obtenerGrupo(@PathVariable String grupo) {
        return service.obtenerGrupo(grupo);
    }

    private static String usuario(JwtAuthenticationToken auth) {
        String correo = auth.getToken().getClaimAsString("preferred_username");
        return correo != null ? correo : auth.getName();
    }
}
