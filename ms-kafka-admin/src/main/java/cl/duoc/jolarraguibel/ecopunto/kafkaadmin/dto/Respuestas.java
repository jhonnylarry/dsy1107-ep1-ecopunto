package cl.duoc.jolarraguibel.ecopunto.kafkaadmin.dto;

import java.util.List;
import java.util.Map;

/** Respuestas de la API de administración. */
public final class Respuestas {

    private Respuestas() {
    }

    public record BrokerInfo(int id, String host, int puerto, boolean controller) {
    }

    public record ClusterInfo(String clusterId, int controllerId, List<BrokerInfo> brokers) {
    }

    public record TopicResumen(String nombre, int particiones, int replicationFactor, boolean protegido) {
    }

    public record ParticionInfo(int particion, Integer lider, List<Integer> replicas, List<Integer> isr) {
    }

    public record TopicDetalle(
            String nombre,
            int particiones,
            int replicationFactor,
            boolean protegido,
            List<ParticionInfo> detalleParticiones,
            Map<String, String> configuracion) {
    }

    public record GrupoResumen(String grupo, String estado, int miembros) {
    }

    public record OffsetParticion(String topic, int particion, Long offsetConfirmado, long offsetFinal, Long lag,
            String consumidor) {
    }

    public record GrupoDetalle(String grupo, String estado, int miembros, long lagTotal, List<OffsetParticion> offsets) {
    }
}
