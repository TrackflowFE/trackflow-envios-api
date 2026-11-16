package com.trackflow.modules.pickups.api.dto;

import com.trackflow.modules.pickups.application.RecogidaSolicitada;
import java.time.Instant;

public record RecogidaAdmitidaResponse(
        String pickupNumber,
        String managementCode,
        String eventId,
        String status,
        Instant requestedAt,
        String processingStatus) {

    public static RecogidaAdmitidaResponse from(RecogidaSolicitada solicitud) {
        return new RecogidaAdmitidaResponse(
                solicitud.pickupNumber(),
                solicitud.managementCode(),
                solicitud.eventId(),
                solicitud.status(),
                solicitud.requestedAt(),
                "QUEUED");
    }
}
