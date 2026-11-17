package com.trackflow.modules.pickups.api.dto;

import com.trackflow.modules.pickups.application.VistaDeRecogida;
import com.trackflow.modules.pickups.domain.Pickup;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Una recogida en el listado de la operación: lo justo para saber qué hay que hacer,
 * sin la dirección ni el contacto.
 */
public record RecogidaResumenResponse(
        String pickupNumber,
        String status,
        Long cityId,
        String cityName,
        LocalDate scheduledDate,
        String timeSlot,
        String shipmentTrackingNumber,
        String assignedUsername,
        Instant updatedAt,
        boolean accessRestricted) {

    public static RecogidaResumenResponse from(VistaDeRecogida vista) {
        Pickup pickup = vista.pickup();

        return new RecogidaResumenResponse(
                pickup.getPickupNumber().value(),
                pickup.getStatus().name(),
                pickup.getCityId(),
                vista.cityName(),
                pickup.getScheduledDate(),
                pickup.getTimeSlot(),
                pickup.getShipmentTrackingNumber(),
                pickup.getAssignedUsername(),
                pickup.getUpdatedAt(),
                !vista.datosPrivadosVisibles());
    }
}
