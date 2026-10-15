package com.trackflow.modules.shipments.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bus de eventos compartido por las tres APIs: un intercambio público donde cada API
 * publica sus eventos de dominio y del que cada una consume los que necesita, con una
 * cola propia. Es distinto de los intercambios de recepción de solicitudes, que son
 * privados de cada API.
 *
 * Esta API publica "envío creado" (shipment.created) y consume "evento logístico
 * registrado" (logistics.event.registered).
 */
@Configuration
public class EventosDeIntegracionRabbitMQConfig {

    public static final String EXCHANGE = "trackflow.events";
    public static final String ENVIO_CREADO = "shipment.created";
    public static final String EVENTO_LOGISTICO_REGISTRADO = "logistics.event.registered";

    public static final String QUEUE_EVENTOS_LOGISTICOS = "shipments.logistics-events.queue";
    public static final String DEAD_LETTER_QUEUE = "shipments.logistics-events.dlq";

    private static final String DEAD_LETTER_KEY = "logistics.event.failed";

    @Bean
    TopicExchange trackflowEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    /** Lo que esta API no logra aplicar termina en la cola de descartados en vez de perderse. */
    @Bean
    Queue shipmentsLogisticsEventsQueue() {
        return QueueBuilder.durable(QUEUE_EVENTOS_LOGISTICOS)
                .deadLetterExchange(ShipmentsRabbitMQConfig.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(DEAD_LETTER_KEY)
                .build();
    }

    @Bean
    Queue shipmentsLogisticsEventsDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding shipmentsLogisticsEventsBinding() {
        return BindingBuilder.bind(shipmentsLogisticsEventsQueue()).to(trackflowEventsExchange())
                .with(EVENTO_LOGISTICO_REGISTRADO);
    }

    @Bean
    Binding shipmentsLogisticsEventsDeadLetterBinding() {
        TopicExchange descartados = new TopicExchange(ShipmentsRabbitMQConfig.DEAD_LETTER_EXCHANGE, true, false);
        return BindingBuilder.bind(shipmentsLogisticsEventsDeadLetterQueue()).to(descartados)
                .with(DEAD_LETTER_KEY);
    }
}
