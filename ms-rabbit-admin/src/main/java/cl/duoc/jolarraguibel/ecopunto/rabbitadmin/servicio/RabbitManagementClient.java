package cl.duoc.jolarraguibel.ecopunto.rabbitadmin.servicio;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente de la API HTTP del plugin Management. AMQP no permite listar recursos,
 * así que los listados se leen desde aquí; la creación y eliminación usan AMQP (AmqpAdmin).
 */
@Component
public class RabbitManagementClient {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final String baseUrl;

    public RabbitManagementClient(AdminProperties props,
            @Value("${spring.rabbitmq.username}") String usuario,
            @Value("${spring.rabbitmq.password}") String clave) {
        this.baseUrl = props.managementUrl();
        this.restClient = RestClient.builder()
                .defaultHeaders(h -> h.setBasicAuth(usuario, clave))
                .build();
    }

    /** URI ya codificada: "%2F" es el vhost "/" y no debe volver a codificarse. */
    private URI uri(String ruta) {
        return URI.create(baseUrl + ruta);
    }

    public List<Map<String, Object>> colas() {
        return restClient.get().uri(uri("/api/queues/%2F")).retrieve().body(LISTA);
    }

    public List<Map<String, Object>> exchanges() {
        return restClient.get().uri(uri("/api/exchanges/%2F")).retrieve().body(LISTA);
    }

    public List<Map<String, Object>> bindings() {
        return restClient.get().uri(uri("/api/bindings/%2F")).retrieve().body(LISTA);
    }
}
