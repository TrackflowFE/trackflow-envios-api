package com.trackflow.modules.chatbot.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record MensajeRequest(
        @NotNull(message = "sesionId es obligatorio") UUID sesionId,
        @NotBlank(message = "El mensaje no puede estar vacío")
        @Size(max = 300, message = "El mensaje admite hasta 300 caracteres") String texto) {
}
