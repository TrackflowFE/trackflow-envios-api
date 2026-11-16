package com.trackflow.modules.pickups.application;

import com.trackflow.shared.events.PickupCompletedEvent;

/**
 * Puerto de publicación del evento de recogida completada.
 *
 * Lo consume logística para dejar el envío en custodia. La dirección del remitente
 * nunca viaja en el evento: solo la ciudad donde se recogió.
 */
public interface RecogidaCompletadaPublisher {

    void publicar(PickupCompletedEvent evento);
}
