package com.trackflow.modules.pqr.api;

import com.trackflow.modules.pqr.domain.PqrNoEncontradaException;
import com.trackflow.modules.pqr.domain.RespuestaRequeridaException;
import com.trackflow.modules.pqr.domain.TransicionDePqrInvalidaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce los errores de las PQR a respuestas problem+json. */
@RestControllerAdvice(assignableTypes = {PqrPublicController.class, PqrGestionController.class})
public class PqrExceptionHandler {

    @ExceptionHandler(PqrNoEncontradaException.class)
    ProblemDetail noEncontrada(PqrNoEncontradaException e) {
        return problema(HttpStatus.NOT_FOUND, "PQR no encontrada", e.getMessage());
    }

    @ExceptionHandler(TransicionDePqrInvalidaException.class)
    ProblemDetail transicionInvalida(TransicionDePqrInvalidaException e) {
        return problema(HttpStatus.CONFLICT, "Cambio de estado no permitido", e.getMessage());
    }

    @ExceptionHandler(RespuestaRequeridaException.class)
    ProblemDetail respuestaRequerida(RespuestaRequeridaException e) {
        return problema(HttpStatus.BAD_REQUEST, "Falta la respuesta", e.getMessage());
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
