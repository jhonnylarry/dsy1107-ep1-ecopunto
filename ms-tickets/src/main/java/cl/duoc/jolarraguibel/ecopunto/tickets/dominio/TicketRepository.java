package cl.duoc.jolarraguibel.ecopunto.tickets.dominio;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByReporteId(Long reporteId);

    List<Ticket> findAllByOrderByIdDesc();
}
