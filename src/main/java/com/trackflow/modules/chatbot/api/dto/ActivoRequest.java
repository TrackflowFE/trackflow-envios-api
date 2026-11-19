package com.trackflow.modules.chatbot.api.dto;

import jakarta.validation.constraints.NotNull;

public record ActivoRequest(@NotNull(message = "activo es obligatorio") Boolean activo) {
}
