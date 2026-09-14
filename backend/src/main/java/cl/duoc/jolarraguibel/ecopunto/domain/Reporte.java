package cl.duoc.jolarraguibel.ecopunto.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "reportes")
public class Reporte {

    public enum Estado {
        PENDIENTE, EN_REVISION, RESUELTO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "punto_limpio_id", nullable = false)
    private PuntoLimpio puntoLimpio;

    @Column(nullable = false)
    private String descripcion;

    @Column(name = "autor_email", nullable = false)
    private String autorEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado = Estado.PENDIENTE;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    public Reporte() {
    }

    public Reporte(PuntoLimpio puntoLimpio, String descripcion, String autorEmail) {
        this.puntoLimpio = puntoLimpio;
        this.descripcion = descripcion;
        this.autorEmail = autorEmail;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PuntoLimpio getPuntoLimpio() {
        return puntoLimpio;
    }

    public void setPuntoLimpio(PuntoLimpio puntoLimpio) {
        this.puntoLimpio = puntoLimpio;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getAutorEmail() {
        return autorEmail;
    }

    public void setAutorEmail(String autorEmail) {
        this.autorEmail = autorEmail;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant creadoEn) {
        this.creadoEn = creadoEn;
    }
}
