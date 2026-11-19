package com.trackflow.modules.chatbot.domain;

public class IntencionDuplicadaException extends RuntimeException {

    public IntencionDuplicadaException(String intencion) {
        super("Ya existe una pregunta con la intención " + intencion);
    }
}
