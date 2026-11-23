package cl.duoc.jolarraguibel.ecopunto.estadisticas.dominio;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Contador agregado, identificado por clave (ej. "estado:creado", "comuna:Ñuñoa"). */
@Entity
@Table(name = "contadores")
public class Contador {

    @Id
    private String clave;

    private long valor;

    protected Contador() {
    }

    public Contador(String clave) {
        this.clave = clave;
    }

    public void incrementar() {
        valor++;
    }

    /** Guarda el valor solo si es mayor al actual (máximos y marcas de tiempo). */
    public boolean subirA(long nuevo) {
        if (nuevo <= valor) {
            return false;
        }
        valor = nuevo;
        return true;
    }

    public String getClave() { return clave; }
    public long getValor() { return valor; }
}
