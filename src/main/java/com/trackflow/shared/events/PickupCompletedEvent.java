package com.trackflow.shared.events;

import java.time.Instant;

/**
 * Evento de integración: lo publica el módulo de recogidas de API 1 y lo consume
 * logística, para dejar el envío en custodia de la empresa.
 *
 * Viaja solo lo mínimo: qué recogida se completó, qué envío es y en qué ciudad se
 * recogió. La dirección y el contacto del remitente nunca salen de la API 1, que es
 * donde vive la recogida; logística no los necesita y no los va a guardar.
 */
public record PickupCompletedEvent(
        String pickupNumber,
        String trackingNumber,
        Long cityId,
        Instant pickedUpAt,
        Instant occurredAt) implements DomainEvent {
}
