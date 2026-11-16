package com.trackflow.modules.pickups.api.dto;

import com.trackflow.modules.pickups.application.VistaDeRecogida;
import com.trackflow.modules.pickups.domain.Pickup;
import com.trackflow.modules.pickups.domain.PickupEvent;
import com.trackflow.modules.pickups.domain.PickupEventType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

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
        boolean assigned,
        String requestedBy,
        Instant requestedAt,
        Instant updatedAt,
        String address,
        String contactName,
        String contactPhone,
        boolean accessRestricted,
        List<EventoResponse> history) {

    private static final Pattern DETALLE_HORARIO = Pattern.compile(
            "De \\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}-\\d{2}:\\d{2} a \\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}-\\d{2}:\\d{2}");

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
                pickup.getAssignedUsername() != null,
                pickup.getRequestedBy(),
                pickup.getRequestedAt(),
                pickup.getUpdatedAt(),
                visible ? pickup.getAddress() : null,
                visible ? pickup.getContactName() : null,
                visible ? pickup.getContactPhone() : null,
                !visible,
                vista.historial().stream().map(evento -> EventoResponse.from(evento, visible)).toList());
    }

    public static RecogidaResponse paraRemitente(VistaDeRecogida vista) {
        RecogidaResponse response = from(vista);
        return new RecogidaResponse(
                response.pickupNumber(), response.status(), response.cityId(), response.cityName(),
                response.scheduledDate(), response.timeSlot(), response.packageSize(), response.weightKg(),
                response.shipmentTrackingNumber(), null, response.assigned(),
                "remitente".equals(response.requestedBy()) ? "remitente" : "operación",
                response.requestedAt(), response.updatedAt(),
                response.address(), response.contactName(), response.contactPhone(), response.accessRestricted(),
                vista.historial().stream().map(evento -> new EventoResponse(
                        evento.getType().name(),
                        evento.getPreviousStatus() == null ? null : evento.getPreviousStatus().name(),
                        evento.getNewStatus() == null ? null : evento.getNewStatus().name(),
                        evento.getType() == PickupEventType.ASSIGNED ? "Asignada a un operador"
                                : evento.getType() == PickupEventType.REJECTED ? "La solicitud no pudo aplicarse"
                                : evento.getType() == PickupEventType.RESCHEDULED && evento.getDetail() != null
                                        && DETALLE_HORARIO.matcher(evento.getDetail()).matches() ? evento.getDetail() : null,
                        "remitente".equals(evento.getActorUsername()) ? "remitente" : "operación",
                        evento.getOccurredAt())).toList());
    }
}
