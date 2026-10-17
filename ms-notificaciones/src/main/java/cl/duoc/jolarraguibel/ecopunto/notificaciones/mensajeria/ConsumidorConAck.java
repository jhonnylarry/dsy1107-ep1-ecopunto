package cl.duoc.jolarraguibel.ecopunto.notificaciones.mensajeria;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.stereotype.Component;

/**
 * Política de confirmación explícita para los consumidores:
 * - procesamiento OK                 -> basicAck
 * - error no recuperable             -> basicNack(requeue=false): directo a la DLQ, se registra en el log
 * - error recuperable                -> reintento con espera creciente; agotados los intentos -> DLQ
 */
@Component
public class ConsumidorConAck {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorConAck.class);

    @FunctionalInterface
    public interface Proceso {
        void ejecutar() throws Exception;
    }

    private final MessagingProperties props;

    public ConsumidorConAck(MessagingProperties props) {
        this.props = props;
    }

    public void procesar(Message message, Channel channel, Proceso proceso) throws IOException {
        MessageProperties mp = message.getMessageProperties();
        long tag = mp.getDeliveryTag();
        String id = mp.getMessageId();
        int maxIntentos = props.reintentos().maxIntentos();
        long espera = props.reintentos().esperaInicial().toMillis();

        for (int intento = 1; ; intento++) {
            try {
                proceso.ejecutar();
                channel.basicAck(tag, false);
                log.info("[ACK] cola={} messageId={} intento={}", mp.getConsumerQueue(), id, intento);
                return;
            } catch (MensajeNoRecuperableException e) {
                channel.basicNack(tag, false, false);
                log.error("[DLQ] cola={} messageId={} motivo=NO_RECUPERABLE: {} | body={}",
                        mp.getConsumerQueue(), id, e.getMessage(), cuerpo(message));
                return;
            } catch (Exception e) {
                if (intento >= maxIntentos) {
                    channel.basicNack(tag, false, false);
                    log.error("[DLQ] cola={} messageId={} motivo=REINTENTOS_AGOTADOS ({}): {}",
                            mp.getConsumerQueue(), id, maxIntentos, e.getMessage());
                    return;
                }
                log.warn("[RETRY] cola={} messageId={} intento {}/{} falló: {}. Reintento en {} ms",
                        mp.getConsumerQueue(), id, intento, maxIntentos, e.getMessage(), espera);
                dormir(espera);
                espera *= 2;
            }
        }
    }

    private static String cuerpo(Message message) {
        String body = new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8);
        return body.length() > 300 ? body.substring(0, 300) + "…" : body;
    }

    private static void dormir(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
