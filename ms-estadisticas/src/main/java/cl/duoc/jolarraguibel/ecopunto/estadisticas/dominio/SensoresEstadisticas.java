package cl.duoc.jolarraguibel.ecopunto.estadisticas.dominio;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Estadísticas de las lecturas de sensores. Todos los contadores son por contenedor
 * (punto limpio + material): como la key del mensaje es el contenedor, cada contador lo
 * actualiza siempre el mismo hilo del listener y no hay incrementos perdidos.
 * Los totales por punto y por material se suman al consultar.
 *
 * Claves:
 *   sensores:medido:{contenedorId}            última medición procesada (epoch ms), para idempotencia
 *   sensores:lecturas:{punto}:{material}      lecturas recibidas
 *   sensores:maximo:{fecha}:{punto}:{material} nivel máximo del día (%)
 */
@Service
public class SensoresEstadisticas {

    private static final Logger log = LoggerFactory.getLogger(SensoresEstadisticas.class);
    private static final String MEDIDO = "sensores:medido:";
    private static final String LECTURAS = "sensores:lecturas:";
    private static final String MAXIMO = "sensores:maximo:";

    private final ContadorRepository contadores;
    private final ZoneId zona;

    public SensoresEstadisticas(ContadorRepository contadores,
            @Value("${ecopunto.sensores.zona-horaria:America/Santiago}") ZoneId zona) {
        this.contadores = contadores;
        this.zona = zona;
    }

    /**
     * Cuenta la lectura una sola vez: si ya se procesó una medición igual o más reciente del mismo
     * contenedor (reentrega o mensaje atrasado), se ignora. Todo va en la misma transacción.
     *
     * @return false si la lectura se ignoró
     */
    @Transactional
    public boolean registrar(LecturaSensor lectura) {
        Contador medido = obtener(MEDIDO + lectura.contenedorId());
        if (!medido.subirA(lectura.medidoEn().toEpochMilli())) {
            log.warn("[DUPLICADO] lectura de {} medida en {} ya contabilizada o atrasada; se ignora",
                    lectura.contenedorId(), lectura.medidoEn());
            return false;
        }
        contadores.save(medido);

        String sufijo = lectura.puntoLimpioId() + ":" + lectura.material();
        Contador lecturas = obtener(LECTURAS + sufijo);
        lecturas.incrementar();
        contadores.save(lecturas);

        LocalDate dia = LocalDate.ofInstant(lectura.medidoEn(), zona);
        Contador maximo = obtener(MAXIMO + dia + ":" + sufijo);
        if (maximo.subirA(Math.round(lectura.nivelLlenado()))) {
            contadores.save(maximo);
        }
        log.info("[ESTADISTICAS] lectura {} punto={} material={} nivel={}", lectura.contenedorId(),
                lectura.puntoLimpioId(), lectura.material(), lectura.nivelLlenado());
        return true;
    }

    public Resumen resumen() {
        Map<String, Long> porPunto = new TreeMap<>();
        Map<String, Long> porMaterial = new TreeMap<>();
        long total = 0;
        for (Contador c : contadores.findByClaveStartingWith(LECTURAS)) {
            String[] partes = c.getClave().substring(LECTURAS.length()).split(":", 2);
            porPunto.merge(partes[0], c.getValor(), Long::sum);
            porMaterial.merge(partes[1], c.getValor(), Long::sum);
            total += c.getValor();
        }
        Map<String, Long> maximoHoy = new TreeMap<>();
        String hoy = MAXIMO + LocalDate.now(zona) + ":";
        for (Contador c : contadores.findByClaveStartingWith(hoy)) {
            String punto = c.getClave().substring(hoy.length()).split(":", 2)[0];
            maximoHoy.merge(punto, c.getValor(), Math::max);
        }
        return new Resumen(total, porPunto, porMaterial, maximoHoy);
    }

    private Contador obtener(String clave) {
        return contadores.findById(clave).orElseGet(() -> new Contador(clave));
    }

    /** Claves de los mapas: id del punto limpio o material. El máximo es el nivel en %. */
    public record Resumen(
            long lecturasTotales,
            Map<String, Long> lecturasPorPunto,
            Map<String, Long> lecturasPorMaterial,
            Map<String, Long> nivelMaximoHoyPorPunto) {
    }
}
