package com.trackflow.modules.pickups.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record ReprogramarRecogidaRequest(
        @NotNull(message = "la fecha es obligatoria")
        LocalDate scheduledDate,
        @NotBlank(message = "la franja horaria es obligatoria")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d-([01]\\d|2[0-3]):[0-5]\\d$",
                message = "la franja debe tener el formato HH:MM-HH:MM")
        String timeSlot,
        String assignedUsername) {

    public ReprogramarRecogidaRequest {
        if (assignedUsername != null && assignedUsername.isBlank()) {
            assignedUsername = null;
        }
    }
}
