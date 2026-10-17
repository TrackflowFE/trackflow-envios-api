package com.trackflow.modules.shipments.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.junit.jupiter.api.Test;

/** Topología de la recepción de solicitudes: lo rechazado debe terminar en la cola de descartados. */
class ShipmentsRabbitMQConfigTest {

    private final ShipmentsRabbitMQConfig config = new ShipmentsRabbitMQConfig();

    @Test
    void losIntercambiosSonDuraderosYNoSeBorranSolos() {
        TopicExchange principal = config.shipmentsExchange();
        TopicExchange descartados = config.shipmentsDeadLetterExchange();

        assertEquals("shipments.topic", principal.getName());
        assertTrue(principal.isDurable());
        assertFalse(principal.isAutoDelete());
        assertEquals("shipments.dlx", descartados.getName());
        assertTrue(descartados.isDurable());
    }

    @Test
    void laColaDeSolicitudesEnviaLoRechazadoAlIntercambioDeDescartados() {
        Queue cola = config.shipmentsRequestsQueue();

        assertEquals("shipments.requests.queue", cola.getName());
        assertTrue(cola.isDurable());
        assertEquals("shipments.dlx", cola.getArguments().get("x-dead-letter-exchange"));
        assertEquals("shipment.requested", cola.getArguments().get("x-dead-letter-routing-key"));
    }

    @Test
    void lasColasQuedanEnlazadasConLaClaveDeSolicitud() {
        Binding solicitudes = config.shipmentsRequestsBinding();
        Binding descartadas = config.shipmentsDeadLetterBinding();

        assertEquals("shipments.requests.queue", solicitudes.getDestination());
        assertEquals("shipments.topic", solicitudes.getExchange());
        assertEquals("shipment.requested", solicitudes.getRoutingKey());
        assertEquals("shipments.requests.dlq", descartadas.getDestination());
        assertEquals("shipments.dlx", descartadas.getExchange());
        assertTrue(config.shipmentsDeadLetterQueue().isDurable());
    }
}
