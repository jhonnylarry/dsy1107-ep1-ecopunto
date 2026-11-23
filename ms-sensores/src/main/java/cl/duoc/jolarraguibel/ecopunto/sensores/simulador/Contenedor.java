package cl.duoc.jolarraguibel.ecopunto.sensores.simulador;

/** Contenedor de un material en un punto limpio, con su nivel de llenado simulado. */
public class Contenedor {

    private final String contenedorId;
    private final Long puntoLimpioId;
    private final String material;
    private double nivel;
    private int ciclosLleno;

    public Contenedor(Long puntoLimpioId, String material, double nivelInicial) {
        this.contenedorId = "PL-" + puntoLimpioId + "-" + material;
        this.puntoLimpioId = puntoLimpioId;
        this.material = material;
        this.nivel = nivelInicial;
    }

    public String getContenedorId() { return contenedorId; }
    public Long getPuntoLimpioId() { return puntoLimpioId; }
    public String getMaterial() { return material; }
    public synchronized double getNivel() { return nivel; }

    /** Avanza un ciclo de llenado. Tras 3 ciclos lleno, la empresa recolectora lo vacía. */
    synchronized double avanzar(double aumento, double nivelTrasVaciado) {
        if (nivel >= 100.0) {
            ciclosLleno++;
            if (ciclosLleno >= 3) {
                ciclosLleno = 0;
                nivel = nivelTrasVaciado;
            }
        } else {
            nivel = Math.min(100.0, nivel + aumento);
        }
        nivel = Math.round(nivel * 10.0) / 10.0;
        return nivel;
    }

    synchronized void fijarNivel(double nuevoNivel) {
        this.nivel = nuevoNivel;
        this.ciclosLleno = 0;
    }
}
