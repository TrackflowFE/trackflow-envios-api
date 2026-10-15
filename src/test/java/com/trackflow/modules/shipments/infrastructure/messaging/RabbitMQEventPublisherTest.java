package com.trackflow.modules.shipments.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.trackflow.shared.events.DomainEvent;
import com.trackflow.shared.events.EnvioCreadoEvent;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class RabbitMQEventPublisherTest {

    private static final Instant AHORA = Instant.parse("2026-10-15T18:00:00Z");

    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final RabbitMQEventPublisher publicador = new RabbitMQEventPublisher(rabbit);
    private final EnvioCreadoEvent envio = new EnvioCreadoEvent("TF1", "REGISTERED", "Ana", 1L, "MEDELLÍN",
            "Luis", "Calle 1", 10L, "BOGOTÁ", AHORA, AHORA);

    @AfterEach
    void limpiarTransaccion() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void sinTransaccionPublicaDeInmediatoConLaClaveDelEnvioCreado() {
        publicador.publish(envio);

        verify(rabbit).convertAndSend("trackflow.events", "shipment.created", envio);
    }

    @Test
    void dentroDeUnaTransaccionEsperaAQueSeConfirme() {
        TransactionSynchronizationManager.initSynchronization();

        publicador.publish(envio);

        verifyNoInteractions(rabbit);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(rabbit).convertAndSend("trackflow.events", "shipment.created", envio);
    }

    @Test
    void rechazaLosEventosQueEstaApiNoPublica() {
        DomainEvent otro = () -> AHORA;

        assertThrows(IllegalArgumentException.class, () -> publicador.publish(otro));
    }
}
