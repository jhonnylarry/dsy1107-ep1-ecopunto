package cl.duoc.jolarraguibel.ecopunto.domain;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DatosDePruebaSeeder implements CommandLineRunner {

    private final PuntoLimpioRepository puntoLimpioRepository;

    public DatosDePruebaSeeder(PuntoLimpioRepository puntoLimpioRepository) {
        this.puntoLimpioRepository = puntoLimpioRepository;
    }

    @Override
    public void run(String... args) {
        if (puntoLimpioRepository.count() > 0) {
            return;
        }

        puntoLimpioRepository.save(new PuntoLimpio(
                "Punto Limpio Ñuñoa",
                "Av. Irarrázaval 3000",
                "Ñuñoa",
                "Papel, cartón, plástico, vidrio",
                -33.4569, -70.5967));

        puntoLimpioRepository.save(new PuntoLimpio(
                "Punto Limpio Providencia",
                "Av. Providencia 1500",
                "Providencia",
                "Vidrio, latas, pilas",
                -33.4260, -70.6100));
    }
}
