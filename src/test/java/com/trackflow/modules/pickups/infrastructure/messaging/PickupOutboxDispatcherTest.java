package com.trackflow.modules.pickups.infrastructure.messaging;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.trackflow.shared.events.PickupCompletedEvent;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class PickupOutboxDispatcherTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final Instant now = Instant.parse("2026-11-16T12:00:00Z");
    private final PickupOutboxDispatcher dispatcher = new PickupOutboxDispatcher(jdbc, rabbit,
            Clock.fixed(now, ZoneOffset.UTC));

    @SuppressWarnings("unchecked")
    private void pendiente() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("pickup_number")).thenReturn("RC0123456789AB");
        when(rs.getString("tracking_number")).thenReturn("TF0123456789AB");
        when(rs.getLong("city_id")).thenReturn(1L);
        when(rs.getTimestamp(anyString())).thenReturn(Timestamp.from(now));
        when(jdbc.query(anyString(), any(RowMapper.class))).thenAnswer(invocation ->
                List.of(((RowMapper<?>) invocation.getArgument(1)).mapRow(rs, 0)));
    }

    @Test
    void marcaPublicadoSoloConConfirmacion() throws Exception {
        pendiente();
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).convertAndSend(anyString(), anyString(), any(PickupCompletedEvent.class), any(CorrelationData.class));
        dispatcher.publicarPendientes();
        verify(jdbc).update(contains("set published_at"), any(Timestamp.class), eq("RC0123456789AB"));
    }

    @Test
    void falloDelBrokerConservaElPendiente() throws Exception {
        pendiente();
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(false, "rechazado"));
            return null;
        }).when(rabbit).convertAndSend(anyString(), anyString(), any(PickupCompletedEvent.class), any(CorrelationData.class));
        dispatcher.publicarPendientes();
        verify(jdbc, never()).update(contains("set published_at"), any(), any());
    }
}
