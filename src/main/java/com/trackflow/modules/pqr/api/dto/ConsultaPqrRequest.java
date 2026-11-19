package com.trackflow.modules.pqr.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConsultaPqrRequest(
        @NotBlank(message = "el radicado es obligatorio") @Size(max = 20, message = "el radicado es demasiado largo")
        String radicado,
        @NotBlank(message = "el correo es obligatorio") @Size(max = 120, message = "el correo es demasiado largo")
        String email) {
}
