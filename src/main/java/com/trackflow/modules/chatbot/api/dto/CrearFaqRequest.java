package com.trackflow.modules.chatbot.api.dto;

import com.trackflow.modules.chatbot.domain.PreguntaFrecuente.Contenido;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CrearFaqRequest(
        @NotBlank @Pattern(regexp = "[a-z][a-z0-9_]{2,59}",
                message = "La intención usa minúsculas, números y guion bajo (3 a 60 caracteres)") String intencion,
        @NotBlank @Size(max = 40) String categoria,
        @NotBlank @Size(max = 200) String pregunta,
        @NotEmpty @Size(max = 15, message = "Máximo 15 palabras clave") List<@NotBlank @Size(max = 30) String> palabrasClave,
        @NotBlank @Size(max = 1000) String respuesta,
        @Min(0) @Max(999) int orden) {

    public Contenido contenido() {
        return new Contenido(categoria, pregunta, palabrasClave, respuesta, orden);
    }
}
