package com.trackflow.modules.chatbot.api.dto;

import com.trackflow.modules.chatbot.domain.PreguntaFrecuente.Contenido;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Igual que crear, sin la intención: es el identificador y no cambia. */
public record EditarFaqRequest(
        @NotBlank @Size(max = 40) String categoria,
        @NotBlank @Size(max = 200) String pregunta,
        @NotEmpty @Size(max = 15, message = "Máximo 15 palabras clave") List<@NotBlank @Size(max = 30) String> palabrasClave,
        @NotBlank @Size(max = 1000) String respuesta,
        @Min(0) @Max(999) int orden) {

    public Contenido contenido() {
        return new Contenido(categoria, pregunta, palabrasClave, respuesta, orden);
    }
}
