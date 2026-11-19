package com.trackflow.modules.pqr.api.dto;

import com.trackflow.modules.pqr.domain.EstadoPqr;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CambiarEstadoRequest(
        @NotNull(message = "el estado es obligatorio") EstadoPqr estado,
        @Size(max = 2000, message = "la respuesta no puede pasar de 2000 caracteres") String respuesta) {
}
