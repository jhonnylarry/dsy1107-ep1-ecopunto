package cl.duoc.jolarraguibel.ecopunto.estadisticas.mensajeria.sensores;

import static org.assertj.core.api.Assertions.assertThat;

import cl.duoc.jolarraguibel.ecopunto.estadisticas.dominio.SensoresEstadisticas;
import java.time.Instant;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Contra un broker embebido: JSON malformado, lectura fuera de rango, key distinta del contenedor y
 * lectura repetida se descartan sin detener el consumidor; las lecturas válidas se cuentan una vez.
 */
@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "ecopunto.sensores.reintentos.espera-inicial=100ms"
})
@EmbeddedKafka(partitions = 3, topics = "ecopunto.sensores.lecturas")
@DirtiesContext
class LecturasSensoresListenerIntegracionTest {

    private static final String TOPIC = "ecopunto.sensores.lecturas";

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private SensoresEstadisticas estadisticas;

    private static String json(String contenedor, String material, double nivel, Instant medidoEn) {
        return """
                {"lecturaId":"%s","contenedorId":"%s","puntoLimpioId":7,"material":"%s",\
                "nivelLlenado":%s,"medidoEn":"%s"}""".formatted(contenedor + "-" + medidoEn.toEpochMilli(),
                contenedor, material, nivel, medidoEn);
    }

    @Test
    void cuentaSoloLasLecturasValidas() throws Exception {
        Map<String, Object> props = KafkaTestUtils.producerProps(broker);
        KafkaTemplate<String, String> productor = new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new StringSerializer()));
        Instant t = Instant.now();

        productor.send(new ProducerRecord<>(TOPIC, "PL-7-VIDRIO", "esto no es json")).get();
        productor.send(new ProducerRecord<>(TOPIC, "PL-7-VIDRIO", json("PL-7-VIDRIO", "VIDRIO", 140, t))).get();
        productor.send(new ProducerRecord<>(TOPIC, "otra-key", json("PL-7-VIDRIO", "VIDRIO", 30, t))).get();
        productor.send(new ProducerRecord<>(TOPIC, "PL-7-VIDRIO", json("PL-7-VIDRIO", "VIDRIO", 45, t))).get();
        productor.send(new ProducerRecord<>(TOPIC, "PL-7-VIDRIO", json("PL-7-VIDRIO", "VIDRIO", 45, t))).get();
        productor.send(new ProducerRecord<>(TOPIC, "PL-7-VIDRIO",
                json("PL-7-VIDRIO", "VIDRIO", 77, t.plusSeconds(5)))).get();
        productor.send(new ProducerRecord<>(TOPIC, "PL-7-PAPEL", json("PL-7-PAPEL", "PAPEL", 12, t))).get();

        SensoresEstadisticas.Resumen resumen = estadisticas.resumen();
        long limite = System.currentTimeMillis() + 30_000;
        while (resumen.lecturasTotales() < 3 && System.currentTimeMillis() < limite) {
            Thread.sleep(200);
            resumen = estadisticas.resumen();
        }

        assertThat(resumen.lecturasTotales()).isEqualTo(3);
        assertThat(resumen.lecturasPorPunto()).containsEntry("7", 3L);
        assertThat(resumen.lecturasPorMaterial()).containsEntry("VIDRIO", 2L).containsEntry("PAPEL", 1L);
        assertThat(resumen.nivelMaximoHoyPorPunto()).containsEntry("7", 77L);
    }
}
