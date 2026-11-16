package com.trackflow.modules.pickups.api.dto;

import jakarta.validation.constraints.Size;

public record CancelarRecogidaRequest(
        @Size(max = 200, message = "el motivo es demasiado largo") String reason) {

    public CancelarRecogidaRequest {
        if (reason != null && reason.isBlank()) {
            reason = null;
        }
    }
}
