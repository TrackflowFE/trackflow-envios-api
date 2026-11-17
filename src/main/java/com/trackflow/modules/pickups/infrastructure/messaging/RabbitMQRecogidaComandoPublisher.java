package com.trackflow.modules.pickups.infrastructure.messaging;

import com.trackflow.modules.pickups.application.RecogidaComando;
import com.trackflow.modules.pickups.application.RecogidaComandoPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
class RabbitMQRecogidaComandoPublisher implements RecogidaComandoPublisher {

    private final RabbitTemplate rabbitTemplate;

    RabbitMQRecogidaComandoPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publicar(RecogidaComando comando) {
        rabbitTemplate.convertAndSend(
                PickupsRabbitMQConfig.EXCHANGE,
                PickupsRabbitMQConfig.ROUTING_KEY,
                RecogidaComandoMensaje.from(comando));
    }
}
