package com.trackflow.modules.pickups.infrastructure;

import com.trackflow.modules.pickups.application.EnviosRegistrados;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Envíos que se pueden vincular a una recogida.
 *
 * Lectura de referencia declarada, como las de cobertura y de red de rutas: el
 * módulo de recogidas solo necesita confirmar que la guía existe y de qué ciudad
 * parte, no el modelo completo del envío.
 */
@Component
class EnviosRegistradosJdbc implements EnviosRegistrados {

    private final JdbcTemplate jdbc;

    EnviosRegistradosJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EnvioRegistrado> buscar(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            return Optional.empty();
        }

        List<EnvioRegistrado> encontrados = jdbc.query(
                "select tracking_number, sender_city_id, status from shipments where tracking_number = ?",
                (rs, fila) -> new EnvioRegistrado(rs.getString("tracking_number"), rs.getLong("sender_city_id"),
                        rs.getString("status")),
                trackingNumber);

        return encontrados.stream().findFirst();
    }
}
