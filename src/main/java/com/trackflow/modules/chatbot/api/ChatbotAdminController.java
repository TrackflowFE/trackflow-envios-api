package com.trackflow.modules.chatbot.api;

import com.trackflow.modules.chatbot.api.dto.ActivoRequest;
import com.trackflow.modules.chatbot.api.dto.CrearFaqRequest;
import com.trackflow.modules.chatbot.api.dto.EditarFaqRequest;
import com.trackflow.modules.chatbot.api.dto.FaqResponse;
import com.trackflow.modules.chatbot.application.AdministrarFaq;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * El editor de preguntas frecuentes. Solo el administrador entra aquí: la regla
 * /api/admin/** de la seguridad ya lo exige. No hay DELETE: se desactiva.
 */
@RestController
@RequestMapping("/api/admin/chatbot/faq")
public class ChatbotAdminController {

    private static final String SIN_TOKEN = "sistema-local";

    private final AdministrarFaq administrar;

    public ChatbotAdminController(AdministrarFaq administrar) {
        this.administrar = administrar;
    }

    @GetMapping
    public List<FaqResponse> listar() {
        return administrar.listar().stream().map(FaqResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<FaqResponse> crear(@Valid @RequestBody CrearFaqRequest request, Principal principal) {
        var faq = administrar.crear(request.intencion(), request.contenido(), actor(principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(FaqResponse.from(faq));
    }

    @PutMapping("/{id}")
    public FaqResponse editar(@PathVariable Long id, @Valid @RequestBody EditarFaqRequest request,
            Principal principal) {
        return FaqResponse.from(administrar.editar(id, request.contenido(), actor(principal)));
    }

    @PostMapping("/{id}/activo")
    public FaqResponse activar(@PathVariable Long id, @Valid @RequestBody ActivoRequest request,
            Principal principal) {
        return FaqResponse.from(administrar.cambiarActivo(id, request.activo(), actor(principal)));
    }

    private static String actor(Principal principal) {
        return principal == null ? SIN_TOKEN : principal.getName();
    }
}
