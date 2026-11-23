package cl.duoc.jolarraguibel.ecopunto.monitoreo.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Estado actual de un contenedor, reconstruido a partir de sus lecturas. */
@Entity
@Table(name = "contenedores")
public class Contenedor {

    @Id
    @Column(name = "contenedor_id")
    private String contenedorId;

    @Column(name = "punto_limpio_id", nullable = false)
    private Long puntoLimpioId;

    @Column(nullable = false)
    private String material;

    @Column(name = "nivel_actual")
    private double nivelActual;

    @Column(name = "nivel_promedio")
    private double nivelPromedio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoLlenado estado = EstadoLlenado.NORMAL;

    private long lecturas;

    @Column(name = "ultima_lectura_id")
    private String ultimaLecturaId;

    @Column(name = "ultima_medicion")
    private Instant ultimaMedicion;

    @Column(name = "cambio_estado_en")
    private Instant cambioEstadoEn;

    protected Contenedor() {
    }

    public Contenedor(String contenedorId, Long puntoLimpioId, String material) {
        this.contenedorId = contenedorId;
        this.puntoLimpioId = puntoLimpioId;
        this.material = material;
    }

    /** La lectura ya se aplicó (reentrega) o es más antigua que la última conocida. */
    boolean yaConsidera(String lecturaId, Instant medidoEn) {
        return lecturaId.equals(ultimaLecturaId)
                || (ultimaMedicion != null && medidoEn.isBefore(ultimaMedicion));
    }

    /** Aplica la lectura y devuelve true si el estado cambió. */
    boolean aplicar(String lecturaId, double nivel, Instant medidoEn, MonitoreoProperties reglas) {
        nivelPromedio = lecturas == 0
                ? nivel
                : reglas.pesoPromedio() * nivel + (1 - reglas.pesoPromedio()) * nivelPromedio;
        nivelPromedio = Math.round(nivelPromedio * 10.0) / 10.0;
        nivelActual = nivel;
        lecturas++;
        ultimaLecturaId = lecturaId;
        ultimaMedicion = medidoEn;

        EstadoLlenado nuevo = reglas.clasificar(nivel);
        boolean cambio = nuevo != estado;
        if (cambio) {
            estado = nuevo;
            cambioEstadoEn = medidoEn;
        }
        return cambio;
    }

    public String getContenedorId() { return contenedorId; }
    public Long getPuntoLimpioId() { return puntoLimpioId; }
    public String getMaterial() { return material; }
    public double getNivelActual() { return nivelActual; }
    public double getNivelPromedio() { return nivelPromedio; }
    public EstadoLlenado getEstado() { return estado; }
    public long getLecturas() { return lecturas; }
    public Instant getUltimaMedicion() { return ultimaMedicion; }
    public Instant getCambioEstadoEn() { return cambioEstadoEn; }
}
