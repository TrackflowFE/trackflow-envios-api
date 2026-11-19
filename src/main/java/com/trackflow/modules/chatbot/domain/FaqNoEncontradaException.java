package com.trackflow.modules.chatbot.domain;

public class FaqNoEncontradaException extends RuntimeException {

    public FaqNoEncontradaException(Long id) {
        super("No existe la pregunta frecuente " + id);
    }
}
