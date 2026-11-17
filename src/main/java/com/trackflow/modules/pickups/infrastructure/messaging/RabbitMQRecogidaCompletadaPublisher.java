package com.trackflow.modules.pickups.infrastructure.messaging;

import com.trackflow.modules.pickups.application.RecogidaCompletadaPublisher;
import com.trackflow.shared.events.PickupCompletedEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.sql.Timestamp;

/**
 * Guarda el evento en la misma transacción que la recogida. Un proceso separado lo
 * publica y lo marca entregado solo tras recibir confirmación del broker.
 */
@Component
class RabbitMQRecogidaCompletadaPublisher implements RecogidaCompletadaPublisher {

    private final JdbcTemplate jdbc;

    RabbitMQRecogidaCompletadaPublisher(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void publicar(PickupCompletedEvent evento) {
        jdbc.update("""
                insert into pickup_outbox (pickup_number, tracking_number, city_id, picked_up_at, occurred_at)
                values (?, ?, ?, ?, ?) on conflict (pickup_number) do nothing
                """, evento.pickupNumber(), evento.trackingNumber(), evento.cityId(),
                Timestamp.from(evento.pickedUpAt()), Timestamp.from(evento.occurredAt()));
    }
}
