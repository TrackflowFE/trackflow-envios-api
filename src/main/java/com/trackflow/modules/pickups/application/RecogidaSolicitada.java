package com.trackflow.modules.pickups.application;

import java.time.Instant;

/**
 * Recogida admitida y pendiente de aplicar.
 *
 * El número ya está generado cuando se devuelve, aunque la recogida todavía no esté
 * en la base: se genera en la admisión para poder entregársela a quien la pide en el
 * momento, igual que la guía de un envío.
 */
public record RecogidaSolicitada(
        String eventId,
        String pickupNumber,
        String status,
        Instant requestedAt,
        String managementCode) {
}
