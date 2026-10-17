package cl.duoc.jolarraguibel.ecopunto.rabbitadmin.servicio;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ecopunto.admin")
public record AdminProperties(String managementUrl, String deadLetterExchange) {
}
