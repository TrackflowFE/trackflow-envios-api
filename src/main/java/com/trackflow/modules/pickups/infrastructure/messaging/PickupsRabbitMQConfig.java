package com.trackflow.modules.pickups.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Mensajería de las recogidas.
 *
 * Son dos caminos distintos y por eso dos tópicos:
 *
 * - El privado (pickups.topic) es donde esta API recibe sus propios comandos. Es
 *   como el de envíos: nadie más publica ahí.
 * - El compartido (trackflow.events) es donde la API publica que una recogida se
 *   completó, para que logística registre el movimiento y el envío entre en custodia.
 *
 * El nombre del intercambio compartido se repite aquí a propósito: es el bus de las
 * tres APIs y su nombre lo fija el contrato entre ellas, no este módulo.
 */
@Configuration
public class PickupsRabbitMQConfig {

    public static final String EXCHANGE = "pickups.topic";
    public static final String ROUTING_KEY = "pickup.command";
    public static final String QUEUE = "pickups.requests.queue";

    public static final String DEAD_LETTER_EXCHANGE = "pickups.dlx";
    public static final String DEAD_LETTER_QUEUE = "pickups.requests.dlq";

    public static final String EVENTOS_EXCHANGE = "trackflow.events";
    public static final String PICKUP_COMPLETED = "pickup.completed";

    @Bean
    TopicExchange pickupsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    TopicExchange pickupsDeadLetterExchange() {
        return new TopicExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue pickupsRequestsQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(ROUTING_KEY)
                .build();
    }

    @Bean
    Queue pickupsDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding pickupsRequestsBinding() {
        return BindingBuilder.bind(pickupsRequestsQueue()).to(pickupsExchange()).with(ROUTING_KEY);
    }

    @Bean
    Binding pickupsDeadLetterBinding() {
        return BindingBuilder.bind(pickupsDeadLetterQueue()).to(pickupsDeadLetterExchange()).with(ROUTING_KEY);
    }
}
