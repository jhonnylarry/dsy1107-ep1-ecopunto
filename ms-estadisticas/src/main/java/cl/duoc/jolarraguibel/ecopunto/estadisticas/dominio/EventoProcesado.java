package cl.duoc.jolarraguibel.ecopunto.estadisticas.dominio;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Registro de eventos ya contabilizados: evita contar dos veces un mensaje reentregado. */
@Entity
@Table(name = "eventos_procesados")
public class EventoProcesado {

    @Id
    private String eventId;

    private String tipo;

    private Instant procesadoEn = Instant.now();

    protected EventoProcesado() {
    }

    public EventoProcesado(String eventId, String tipo) {
        this.eventId = eventId;
        this.tipo = tipo;
    }

    public String getEventId() { return eventId; }
    public String getTipo() { return tipo; }
    public Instant getProcesadoEn() { return procesadoEn; }
}
