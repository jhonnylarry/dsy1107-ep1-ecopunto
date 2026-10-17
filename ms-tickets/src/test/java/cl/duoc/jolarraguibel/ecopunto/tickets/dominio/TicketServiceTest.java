package cl.duoc.jolarraguibel.ecopunto.tickets.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class TicketServiceTest {

    private final TicketRepository repository = mock(TicketRepository.class);
    private final TicketService service = new TicketService(repository);

    private static SolicitudTicket solicitud(String descripcion) {
        return new SolicitudTicket("msg-1", 7L, 1L, "Punto Limpio Ñuñoa", "Av. Irarrázaval 3000",
                "Ñuñoa", descripcion, "encargado@ecopunto.cl");
    }

    @Test
    void generaTicketConPrioridadAltaSiLaDescripcionEsUrgente() {
        when(repository.findByReporteId(7L)).thenReturn(Optional.empty());
        when(repository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket ticket = service.generar(solicitud("El contenedor está rebalsado"));

        assertThat(ticket.getPrioridad()).isEqualTo(Ticket.Prioridad.ALTA);
        assertThat(ticket.getReporteId()).isEqualTo(7L);
    }

    @Test
    void generaTicketConPrioridadNormal() {
        when(repository.findByReporteId(7L)).thenReturn(Optional.empty());
        when(repository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.generar(solicitud("Falta señalética")).getPrioridad())
                .isEqualTo(Ticket.Prioridad.NORMAL);
    }

    @Test
    void esIdempotenteSiElReporteYaTieneTicket() {
        Ticket existente = new Ticket(7L, 1L, "Punto Limpio Ñuñoa", "x", "Ñuñoa", "texto",
                Ticket.Prioridad.NORMAL, "encargado@ecopunto.cl", "msg-original");
        when(repository.findByReporteId(7L)).thenReturn(Optional.of(existente));

        Ticket resultado = service.generar(solicitud("mensaje reentregado"));

        assertThat(resultado).isSameAs(existente);
        verify(repository, never()).save(any());
    }
}
