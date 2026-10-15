package com.trackflow.modules.shipments.infrastructure.messaging;

import com.trackflow.shared.events.DomainEvent;
import com.trackflow.shared.events.EnvioCreadoEvent;
import com.trackflow.shared.events.EventPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publica los eventos de dominio de esta API en el bus compartido (RabbitMQ). Si hay una
 * transacción en curso espera a que termine con éxito, igual que lo hacía el bus interno
 * con AFTER_COMMIT: así nadie se entera de un envío que luego no llegó a guardarse.
 */
@Component
class RabbitMQEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbit;

    RabbitMQEventPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @Override
    public void publish(DomainEvent event) {
        String routingKey = routingKey(event);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviar(routingKey, event);
                }
            });
            return;
        }
        enviar(routingKey, event);
    }

    private void enviar(String routingKey, DomainEvent event) {
        rabbit.convertAndSend(EventosDeIntegracionRabbitMQConfig.EXCHANGE, routingKey, event);
    }

    private static String routingKey(DomainEvent event) {
        if (event instanceof EnvioCreadoEvent) {
            return EventosDeIntegracionRabbitMQConfig.ENVIO_CREADO;
        }
        throw new IllegalArgumentException(
                "Esta API no publica eventos de tipo " + event.getClass().getSimpleName());
    }
}
