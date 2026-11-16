package com.trackflow.modules.pickups.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VincularEnvioRequest(
        @NotBlank(message = "el número de seguimiento es obligatorio")
        @Pattern(regexp = "^TF[0-9A-Z]{12}$", message = "el número de seguimiento no tiene el formato de una guía")
        String trackingNumber) {
}
