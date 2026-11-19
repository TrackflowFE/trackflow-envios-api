package com.trackflow.modules.chatbot.api;

import com.trackflow.modules.chatbot.application.DemasiadosMensajesException;
import com.trackflow.modules.chatbot.application.SesionInvalidaException;
import com.trackflow.modules.chatbot.domain.FaqNoEncontradaException;
import com.trackflow.modules.chatbot.domain.IntencionDuplicadaException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce los errores del chatbot a respuestas problem+json. */
@RestControllerAdvice(assignableTypes = {ChatbotController.class, ChatbotAdminController.class})
public class ChatbotExceptionHandler {

    @ExceptionHandler(SesionInvalidaException.class)
    ProblemDetail sesionInvalida(SesionInvalidaException e) {
        return problema(HttpStatus.BAD_REQUEST, "Sesión no válida", e.getMessage());
    }

    @ExceptionHandler(DemasiadosMensajesException.class)
    ResponseEntity<ProblemDetail> demasiados(DemasiadosMensajesException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(Math.max(1, e.segundos())))
                .body(problema(HttpStatus.TOO_MANY_REQUESTS, "Demasiados mensajes", e.getMessage()));
    }

    @ExceptionHandler(FaqNoEncontradaException.class)
    ProblemDetail noEncontrada(FaqNoEncontradaException e) {
        return problema(HttpStatus.NOT_FOUND, "Pregunta no encontrada", e.getMessage());
    }

    @ExceptionHandler(IntencionDuplicadaException.class)
    ProblemDetail duplicada(IntencionDuplicadaException e) {
        return problema(HttpStatus.CONFLICT, "Intención repetida", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail solicitudInvalida(IllegalArgumentException e) {
        return problema(HttpStatus.BAD_REQUEST, "Solicitud inválida", e.getMessage());
    }

    private static ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        return problema;
    }
}
