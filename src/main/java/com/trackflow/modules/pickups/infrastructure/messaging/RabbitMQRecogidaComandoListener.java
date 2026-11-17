package com.trackflow.modules.pickups.infrastructure.messaging;

import com.trackflow.modules.pickups.application.EjecutarComandoDeRecogida;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada por mensajería: las recogidas se registran y se cambian al
 * consumir el comando de la cola, no en la petición HTTP que lo admite.
 */
@Component
class RabbitMQRecogidaComandoListener {

    private final EjecutarComandoDeRecogida ejecutar;

    RabbitMQRecogidaComandoListener(EjecutarComandoDeRecogida ejecutar) {
        this.ejecutar = ejecutar;
    }

    @RabbitListener(queues = PickupsRabbitMQConfig.QUEUE)
    public void recibir(RecogidaComandoMensaje mensaje) {
        ejecutar.ejecutar(mensaje.toComando());
    }
}
