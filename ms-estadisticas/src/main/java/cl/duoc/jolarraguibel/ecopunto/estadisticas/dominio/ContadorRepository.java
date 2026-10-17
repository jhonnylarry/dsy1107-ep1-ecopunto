package cl.duoc.jolarraguibel.ecopunto.estadisticas.dominio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContadorRepository extends JpaRepository<Contador, String> {

    List<Contador> findByClaveStartingWith(String prefijo);
}
