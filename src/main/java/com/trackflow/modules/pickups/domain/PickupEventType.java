package com.trackflow.modules.pickups.domain;

/**
 * Qué pasó en la recogida. Cada cambio deja una fila en pickup_events con el tipo de
 * cambio, el estado anterior y el nuevo, de modo que la bitácora dice no solo cómo
 * está la recogida ahora, sino cómo llegó a estar así.
 */
public enum PickupEventType {
    CREATED,
    SCHEDULED,
    ASSIGNED,
    CANCELLED,
    DISPATCHED,
    COMPLETED,
    SHIPMENT_LINKED
}
