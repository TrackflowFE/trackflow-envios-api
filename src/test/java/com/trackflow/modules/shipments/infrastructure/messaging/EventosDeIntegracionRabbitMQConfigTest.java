package com.trackflow.modules.shipments.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

/** Bus compartido: esta API consume los eventos logísticos con una cola propia y su cola de descartados. */
class EventosDeIntegracionRabbitMQConfigTest {

    private final EventosDeIntegracionRabbitMQConfig config = new EventosDeIntegracionRabbitMQConfig();

    @Test
    void elIntercambioCompartidoEsDuradero() {
        TopicExchange intercambio = config.trackflowEventsExchange();

        assertEquals("trackflow.events", intercambio.getName());
        assertTrue(intercambio.isDurable());
    }

    @Test
    void laColaDeEventosLogisticosMandaLosFallidosALaColaDeDescartados() {
        Queue cola = config.shipmentsLogisticsEventsQueue();

        assertEquals("shipments.logistics-events.queue", cola.getName());
        assertEquals(ShipmentsRabbitMQConfig.DEAD_LETTER_EXCHANGE, cola.getArguments().get("x-dead-letter-exchange"));
        assertEquals("logistics.event.failed", cola.getArguments().get("x-dead-letter-routing-key"));
    }

    @Test
    void consumeSoloLaClaveDeEventoLogisticoRegistrado() {
        Binding enlace = config.shipmentsLogisticsEventsBinding();

        assertEquals("shipments.logistics-events.queue", enlace.getDestination());
        assertEquals("trackflow.events", enlace.getExchange());
        assertEquals("logistics.event.registered", enlace.getRoutingKey());
    }

    @Test
    void losDescartadosLleganASuColaPorLaClaveDeFallo() {
        Binding enlace = config.shipmentsLogisticsEventsDeadLetterBinding();

        assertEquals("shipments.logistics-events.dlq", enlace.getDestination());
        assertEquals("shipments.dlx", enlace.getExchange());
        assertEquals("logistics.event.failed", enlace.getRoutingKey());
        assertTrue(config.shipmentsLogisticsEventsDeadLetterQueue().isDurable());
    }
}
