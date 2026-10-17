package cl.duoc.jolarraguibel.ecopunto.tickets.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * Orden de trabajo para el equipo de terreno. Un reporte genera como máximo un ticket
 * (restricción única sobre reporte_id: base de la idempotencia del consumidor).
 */
@Entity
@Table(name = "tickets", uniqueConstraints = @UniqueConstraint(name = "uk_ticket_reporte", columnNames = "reporte_id"))
public class Ticket {

    public enum Prioridad { NORMAL, ALTA }

    public enum Estado { ABIERTO, CERRADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporte_id", nullable = false)
    private Long reporteId;

    @Column(name = "punto_limpio_id", nullable = false)
    private Long puntoLimpioId;

    @Column(name = "punto_nombre", nullable = false)
    private String puntoNombre;

    private String direccion;

    private String comuna;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Prioridad prioridad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado = Estado.ABIERTO;

    @Column(name = "solicitado_por")
    private String solicitadoPor;

    @Column(name = "message_id")
    private String messageId;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    protected Ticket() {
    }

    public Ticket(Long reporteId, Long puntoLimpioId, String puntoNombre, String direccion, String comuna,
            String descripcion, Prioridad prioridad, String solicitadoPor, String messageId) {
        this.reporteId = reporteId;
        this.puntoLimpioId = puntoLimpioId;
        this.puntoNombre = puntoNombre;
        this.direccion = direccion;
        this.comuna = comuna;
        this.descripcion = descripcion;
        this.prioridad = prioridad;
        this.solicitadoPor = solicitadoPor;
        this.messageId = messageId;
    }

    /** Código legible del ticket, derivado del id: TK-000001. */
    public String getCodigo() {
        return id == null ? null : String.format("TK-%06d", id);
    }

    public Long getId() { return id; }
    public Long getReporteId() { return reporteId; }
    public Long getPuntoLimpioId() { return puntoLimpioId; }
    public String getPuntoNombre() { return puntoNombre; }
    public String getDireccion() { return direccion; }
    public String getComuna() { return comuna; }
    public String getDescripcion() { return descripcion; }
    public Prioridad getPrioridad() { return prioridad; }
    public Estado getEstado() { return estado; }
    public String getSolicitadoPor() { return solicitadoPor; }
    public String getMessageId() { return messageId; }
    public Instant getCreadoEn() { return creadoEn; }
}
