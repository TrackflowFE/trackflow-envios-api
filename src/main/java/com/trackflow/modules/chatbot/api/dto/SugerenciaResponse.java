package com.trackflow.modules.chatbot.api.dto;

import com.trackflow.modules.chatbot.application.CatalogoDeFaq;

public record SugerenciaResponse(String intencion, String pregunta) {

    public static SugerenciaResponse from(CatalogoDeFaq.Entrada entrada) {
        return new SugerenciaResponse(entrada.intencion(), entrada.pregunta());
    }
}
