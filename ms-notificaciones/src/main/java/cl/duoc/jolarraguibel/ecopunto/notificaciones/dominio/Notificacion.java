package cl.duoc.jolarraguibel.ecopunto.notificaciones.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Registro de cada notificación enviada. event_id es único: si el mismo evento
 * llega dos veces, no se envía una segunda notificación.
 */
@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(nullable = false)
    private String destinatario;

    @Column(nullable = false)
    private String asunto;

    @Column(nullable = false, length = 1000)
    private String cuerpo;

    @Column(name = "enviada_en", nullable = false)
    private Instant enviadaEn = Instant.now();

    protected Notificacion() {
    }

    public Notificacion(String eventId, String destinatario, String asunto, String cuerpo) {
        this.eventId = eventId;
        this.destinatario = destinatario;
        this.asunto = asunto;
        this.cuerpo = cuerpo;
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getDestinatario() { return destinatario; }
    public String getAsunto() { return asunto; }
    public String getCuerpo() { return cuerpo; }
    public Instant getEnviadaEn() { return enviadaEn; }
}
