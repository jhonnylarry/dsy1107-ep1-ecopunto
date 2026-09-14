package cl.duoc.jolarraguibel.ecopunto.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "puntos_limpios")
public class PuntoLimpio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String direccion;

    @Column(nullable = false)
    private String comuna;

    @Column(name = "materiales_aceptados")
    private String materialesAceptados;

    private Double latitud;

    private Double longitud;

    public PuntoLimpio() {
    }

    public PuntoLimpio(String nombre, String direccion, String comuna,
                        String materialesAceptados, Double latitud, Double longitud) {
        this.nombre = nombre;
        this.direccion = direccion;
        this.comuna = comuna;
        this.materialesAceptados = materialesAceptados;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getComuna() {
        return comuna;
    }

    public void setComuna(String comuna) {
        this.comuna = comuna;
    }

    public String getMaterialesAceptados() {
        return materialesAceptados;
    }

    public void setMaterialesAceptados(String materialesAceptados) {
        this.materialesAceptados = materialesAceptados;
    }

    public Double getLatitud() {
        return latitud;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }
}
