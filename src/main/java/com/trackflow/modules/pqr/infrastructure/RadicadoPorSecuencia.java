package com.trackflow.modules.pqr.infrastructure;

import com.trackflow.modules.pqr.application.GeneradorDeRadicado;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Toma el número de una secuencia de la base de datos, que no repite aunque varias
 * instancias radiquen a la vez. La secuencia no se reinicia con el año: el año del
 * radicado es informativo y el número solo crece.
 */
@Component
public class RadicadoPorSecuencia implements GeneradorDeRadicado {

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public RadicadoPorSecuencia(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public String siguiente() {
        Long numero = jdbc.queryForObject("select nextval('pqr_radicado_seq')", Long.class);
        return formatear(LocalDate.now(clock).getYear(), numero);
    }

    static String formatear(int anio, long numero) {
        return "PQR-%d-%05d".formatted(anio, numero);
    }
}
