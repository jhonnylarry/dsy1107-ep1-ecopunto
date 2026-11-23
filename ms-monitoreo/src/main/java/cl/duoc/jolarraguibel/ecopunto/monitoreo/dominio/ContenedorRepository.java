package cl.duoc.jolarraguibel.ecopunto.monitoreo.dominio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContenedorRepository extends JpaRepository<Contenedor, String> {

    List<Contenedor> findAllByOrderByNivelActualDesc();
}
