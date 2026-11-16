package com.trackflow.modules.pickups.api.dto;

import com.trackflow.modules.pickups.application.VistaDeRecogida;
import com.trackflow.modules.pickups.domain.Pickup;
import com.trackflow.modules.pickups.domain.PickupEvent;
import com.trackflow.modules.pickups.domain.PickupEventType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * La recogida tal como la ve quien consulta.
 *
 * Cuando el solicitante no tiene la recogida asignada, la dirección y el contacto
 * llegan vacíos y {@code accessRestricted} en true: la operación sigue siendo
 * válida, lo que no se puede ver es el domicilio.
 */
public record RecogidaResponse(
        String pickupNumber,
        String status,
        Long cityId,
        String cityName,
        LocalDate scheduledDate,
        String timeSlot,
        String packageSize,
        BigDecimal weightKg,
        String shipmentTrackingNumber,
        String assignedUsername,
        String requestedBy,
        Instant requestedAt,
        Instant updatedAt,
        String address,
        String contactName,
        String contactPhone,
        boolean accessRestricted,
        List<EventoResponse> history) {

    /** Un cambio de la bitácora de la recogida. */
    public record EventoResponse(
            String type,
            String previousStatus,
            String newStatus,
            String detail,
            String actor,
            Instant occurredAt) {

        static EventoResponse from(PickupEvent evento, boolean visible) {
            return new EventoResponse(
                    evento.getType().name(),
                    evento.getPreviousStatus() == null ? null : evento.getPreviousStatus().name(),
                    evento.getNewStatus() == null ? null : evento.getNewStatus().name(),
                    visible || evento.getType() != PickupEventType.CANCELLED ? evento.getDetail() : null,
                    evento.getActorUsername(),
                    evento.getOccurredAt());
        }
    }

    public static RecogidaResponse from(VistaDeRecogida vista) {
        Pickup pickup = vista.pickup();
        boolean visible = vista.datosPrivadosVisibles();

        return new RecogidaResponse(
                pickup.getPickupNumber().value(),
                pickup.getStatus().name(),
                pickup.getCityId(),
                vista.cityName(),
                pickup.getScheduledDate(),
                pickup.getTimeSlot(),
                pickup.getPackageSize().name(),
                pickup.getWeightKg(),
                pickup.getShipmentTrackingNumber(),
                pickup.getAssignedUsername(),
                pickup.getRequestedBy(),
                pickup.getRequestedAt(),
                pickup.getUpdatedAt(),
                visible ? pickup.getAddress() : null,
                visible ? pickup.getContactName() : null,
                visible ? pickup.getContactPhone() : null,
                !visible,
                vista.historial().stream().map(evento -> EventoResponse.from(evento, visible)).toList());
    }
}
