package cl.duoc.jolarraguibel.ecopunto.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReporteRepository extends JpaRepository<Reporte, Long> {

    List<Reporte> findByPuntoLimpioId(Long puntoLimpioId);
}
