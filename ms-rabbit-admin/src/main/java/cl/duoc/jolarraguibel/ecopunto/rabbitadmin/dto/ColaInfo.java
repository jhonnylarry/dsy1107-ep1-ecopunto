package cl.duoc.jolarraguibel.ecopunto.rabbitadmin.dto;

import java.util.Map;

public record ColaInfo(String nombre, String tipo, boolean durable, long mensajes, long consumidores,
        Map<String, Object> argumentos) {
}
