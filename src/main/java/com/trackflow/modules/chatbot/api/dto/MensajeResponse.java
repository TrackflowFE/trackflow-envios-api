package com.trackflow.modules.chatbot.api.dto;

import com.trackflow.modules.chatbot.application.RespuestaDelChat;
import java.util.List;

public record MensajeResponse(String resultado, String intencion, String texto, List<AccionResponse> acciones) {

    public record AccionResponse(String tipo, String etiqueta) {
    }

    public static MensajeResponse from(RespuestaDelChat respuesta) {
        return new MensajeResponse(respuesta.resultado().name(), respuesta.intencion(), respuesta.texto(),
                respuesta.acciones().stream().map(a -> new AccionResponse(a.tipo().name(), a.etiqueta())).toList());
    }
}
