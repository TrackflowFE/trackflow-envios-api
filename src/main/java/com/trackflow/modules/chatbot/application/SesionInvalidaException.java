package com.trackflow.modules.chatbot.application;

public class SesionInvalidaException extends RuntimeException {

    public SesionInvalidaException() {
        super("La sesión no existe o venció. Crea una nueva.");
    }
}
