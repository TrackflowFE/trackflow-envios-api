package com.trackflow.modules.chatbot.api.dto;

import com.trackflow.modules.chatbot.domain.PreguntaFrecuente;
import java.time.Instant;
import java.util.List;

public record FaqResponse(Long id, String intencion, String categoria, String pregunta, List<String> palabrasClave,
        String respuesta, boolean activo, int orden, Instant actualizadaEn, String actualizadaPor) {

    public static FaqResponse from(PreguntaFrecuente faq) {
        return new FaqResponse(faq.id(), faq.intencion(), faq.categoria(), faq.pregunta(), faq.palabrasClave(),
                faq.respuesta(), faq.activo(), faq.orden(), faq.updatedAt(), faq.updatedBy());
    }
}
