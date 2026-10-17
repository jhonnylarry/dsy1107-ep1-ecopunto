package cl.duoc.jolarraguibel.ecopunto.tickets.mensajeria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.rabbitmq.client.Channel;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

/** Verifica la política de confirmación: ACK, NACK directo a DLQ y reintentos. */
class ConsumidorConAckTest {

    private static final long TAG = 42L;

    private Channel channel;
    private Message message;
    private ConsumidorConAck consumidor;

    @BeforeEach
    void setUp() {
        channel = mock(Channel.class);
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(TAG);
        props.setMessageId("msg-1");
        props.setConsumerQueue("tickets.mantencion");
        message = new Message("{}".getBytes(), props);
        consumidor = new ConsumidorConAck(new MessagingProperties(null, "tickets.mantencion",
                "tickets.mantencion.dlq", "ticket.mantencion",
                new MessagingProperties.Reintentos(3, Duration.ofMillis(1))));
    }

    @Test
    void procesamientoCorrectoHaceAck() throws Exception {
        consumidor.procesar(message, channel, () -> { });

        verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void errorNoRecuperableVaDirectoALaDlqSinReintentar() throws Exception {
        AtomicInteger intentos = new AtomicInteger();

        consumidor.procesar(message, channel, () -> {
            intentos.incrementAndGet();
            throw new MensajeNoRecuperableException("JSON inválido");
        });

        assertThat(intentos).hasValue(1);
        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void errorRecuperableSeReintentaYLuegoVaALaDlq() throws Exception {
        AtomicInteger intentos = new AtomicInteger();

        consumidor.procesar(message, channel, () -> {
            intentos.incrementAndGet();
            throw new IllegalStateException("base de datos no disponible");
        });

        assertThat(intentos).hasValue(3);
        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void errorRecuperableQueSeResuelveEnElReintentoTerminaEnAck() throws Exception {
        AtomicInteger intentos = new AtomicInteger();

        consumidor.procesar(message, channel, () -> {
            if (intentos.incrementAndGet() == 1) {
                throw new IllegalStateException("falla temporal");
            }
        });

        assertThat(intentos).hasValue(2);
        verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }
}
