package cl.duoc.jolarraguibel.ecopunto.notificaciones.dominio;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    boolean existsByEventId(String eventId);
}
