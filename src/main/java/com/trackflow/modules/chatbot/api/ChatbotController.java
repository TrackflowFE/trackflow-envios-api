package com.trackflow.modules.chatbot.api;

import com.trackflow.modules.chatbot.api.dto.MensajeRequest;
import com.trackflow.modules.chatbot.api.dto.MensajeResponse;
import com.trackflow.modules.chatbot.api.dto.SesionResponse;
import com.trackflow.modules.chatbot.api.dto.SugerenciaResponse;
import com.trackflow.modules.chatbot.application.CatalogoDeFaq;
import com.trackflow.modules.chatbot.application.ResponderMensaje;
import com.trackflow.modules.chatbot.application.SesionesDeChat;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lo que usa quien visita la página, sin cuenta. */
@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private static final int MAXIMO_SUGERENCIAS = 6;

    private final SesionesDeChat sesiones;
    private final ResponderMensaje responder;
    private final CatalogoDeFaq catalogo;

    public ChatbotController(SesionesDeChat sesiones, ResponderMensaje responder, CatalogoDeFaq catalogo) {
        this.sesiones = sesiones;
        this.responder = responder;
        this.catalogo = catalogo;
    }

    @PostMapping("/sesiones")
    public ResponseEntity<SesionResponse> crearSesion() {
        return ResponseEntity.status(HttpStatus.CREATED).body(new SesionResponse(sesiones.crear()));
    }

    @PostMapping("/mensajes")
    public MensajeResponse mensaje(@Valid @RequestBody MensajeRequest request) {
        return MensajeResponse.from(responder.responder(request.sesionId(), request.texto()));
    }

    @GetMapping("/sugerencias")
    public List<SugerenciaResponse> sugerencias() {
        return catalogo.sugerencias(MAXIMO_SUGERENCIAS).stream().map(SugerenciaResponse::from).toList();
    }
}
