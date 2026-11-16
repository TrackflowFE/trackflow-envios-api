package com.trackflow.modules.pickups.infrastructure.messaging;

import com.trackflow.shared.events.PickupCompletedEvent;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class PickupOutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(PickupOutboxDispatcher.class);
    private final JdbcTemplate jdbc;
    private final RabbitTemplate rabbit;
    private final Clock clock;

    @Autowired
    PickupOutboxDispatcher(JdbcTemplate jdbc, ConnectionFactory connectionFactory, MessageConverter converter, Clock clock) {
        this.jdbc = jdbc;
        this.rabbit = new RabbitTemplate(connectionFactory);
        this.rabbit.setMessageConverter(converter);
        this.rabbit.setMandatory(true);
        this.rabbit.setReturnsCallback(returned -> log.warn("Evento de recogida sin ruta: {}", returned.getReplyText()));
        this.clock = clock;
    }

    PickupOutboxDispatcher(JdbcTemplate jdbc, RabbitTemplate rabbit, Clock clock) {
        this.jdbc = jdbc;
        this.rabbit = rabbit;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${trackflow.pickups.outbox-delay-ms:5000}")
    @Transactional
    public void publicarPendientes() {
        List<PickupCompletedEvent> pendientes = jdbc.query("""
                select pickup_number, tracking_number, city_id, picked_up_at, occurred_at
                from pickup_outbox where published_at is null order by occurred_at
                limit 20 for update skip locked
                """, (rs, row) -> new PickupCompletedEvent(rs.getString("pickup_number"),
                rs.getString("tracking_number"), rs.getLong("city_id"),
                rs.getTimestamp("picked_up_at").toInstant(), rs.getTimestamp("occurred_at").toInstant()));

        for (PickupCompletedEvent evento : pendientes) {
            try {
                CorrelationData correlation = new CorrelationData(UUID.randomUUID().toString());
                rabbit.convertAndSend(PickupsRabbitMQConfig.EVENTOS_EXCHANGE,
                        PickupsRabbitMQConfig.PICKUP_COMPLETED, evento, correlation);
                CorrelationData.Confirm confirm = correlation.getFuture().get(10, TimeUnit.SECONDS);
                if (!confirm.isAck() || correlation.getReturned() != null) {
                    log.warn("No se confirmó el evento de recogida {}: {}", evento.pickupNumber(), confirm.getReason());
                    continue;
                }
                jdbc.update("update pickup_outbox set published_at = ? where pickup_number = ?",
                        Timestamp.from(clock.instant()), evento.pickupNumber());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("No se pudo publicar la recogida {}; se reintentará", evento.pickupNumber(), e);
            }
        }
    }
}
