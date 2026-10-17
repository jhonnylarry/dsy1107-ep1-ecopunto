package cl.duoc.jolarraguibel.ecopunto.notificaciones.dominio;

import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de notificaciones. El envío de correo se simula con un log y queda registrado en la base.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final NotificacionRepository repository;
    private final String correoEncargados;

    public NotificacionService(NotificacionRepository repository,
            @Value("${ecopunto.notificaciones.correo-encargados}") String correoEncargados) {
        this.repository = repository;
        this.correoEncargados = correoEncargados;
    }

    /** Aviso al equipo de encargados: llegó un reporte nuevo. */
    @Transactional
    public void avisarReporteNuevo(String eventId, Long reporteId, String puntoNombre, String comuna,
            String descripcion, String autorEmail) {
        enviar(eventId, correoEncargados,
                "Nuevo reporte #" + reporteId + " en " + puntoNombre,
                "Se reportó un problema en " + puntoNombre + " (" + comuna + "): \"" + descripcion
                        + "\". Reportado por " + autorEmail + ".");
    }

    /** Aviso al vecino que reportó: su reporte quedó resuelto. */
    @Transactional
    public void avisarReporteResuelto(String eventId, Long reporteId, String puntoNombre, String autorEmail) {
        enviar(eventId, autorEmail,
                "Tu reporte #" + reporteId + " fue resuelto",
                "El problema que reportaste en " + puntoNombre + " fue resuelto. ¡Gracias por ayudar a reciclar mejor!");
    }

    private void enviar(String eventId, String destinatario, String asunto, String cuerpo) {
        if (repository.existsByEventId(eventId)) {
            log.warn("[DUPLICADO] evento {} ya fue notificado; no se reenvía", eventId);
            return;
        }
        if (destinatario == null || !CORREO.matcher(destinatario).matches()) {
            throw new DestinatarioInvalidoException(destinatario);
        }
        repository.save(new Notificacion(eventId, destinatario, asunto, cuerpo));
        log.info("[NOTIFICACION] para={} | asunto=\"{}\" | {}", destinatario, asunto, cuerpo);
    }
}
