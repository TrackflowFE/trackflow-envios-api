package com.trackflow.modules.pickups.application;

import com.trackflow.modules.pickups.domain.PackageSize;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Un cambio en una recogida, pendiente de aplicar.
 *
 * Es el contrato de ingesta del módulo: la petición HTTP valida y publica el
 * comando, y el consumidor de la cola lo aplica. Los campos que cada comando usa son
 * los suyos; los demás vienen en null.
 */
public record RecogidaComando(
        String eventId,
        Tipo comando,
        String pickupNumber,
        String actor,
        Instant occurredAt,
        String address,
        Long cityId,
        String contactName,
        String contactPhone,
        LocalDate scheduledDate,
        String timeSlot,
        PackageSize packageSize,
        BigDecimal weightKg,
        String shipmentTrackingNumber,
        String assignedUsername,
        String reason) {

    /** Las operaciones que se pueden pedir sobre una recogida. */
    public enum Tipo {
        CREAR,
        REPROGRAMAR,
        CANCELAR,
        DESPACHAR,
        COMPLETAR,
        VINCULAR
    }
}
