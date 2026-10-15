package com.trackflow.modules.shipments.infrastructure;

import com.trackflow.modules.shipments.application.AplicarEventoLogistico;
import com.trackflow.modules.shipments.infrastructure.messaging.EventosDeIntegracionRabbitMQConfig;
import com.trackflow.shared.events.EventoLogisticoRegistradoEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Recibe del bus los eventos logísticos registrados por la API de logística y los aplica
 * al envío correspondiente. Si falla, el mensaje pasa a la cola de descartados.
 */
@Component
public class EventoLogisticoSubscriber {

    private final AplicarEventoLogistico aplicarEventoLogistico;

    public EventoLogisticoSubscriber(AplicarEventoLogistico aplicarEventoLogistico) {
        this.aplicarEventoLogistico = aplicarEventoLogistico;
    }

    @RabbitListener(queues = EventosDeIntegracionRabbitMQConfig.QUEUE_EVENTOS_LOGISTICOS)
    public void on(EventoLogisticoRegistradoEvent event) {
        aplicarEventoLogistico.ejecutar(event);
    }
}
