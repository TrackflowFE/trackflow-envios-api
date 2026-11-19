package com.trackflow.modules.chatbot.application;

public class DemasiadosMensajesException extends RuntimeException {

    private final long segundos;

    public DemasiadosMensajesException(long segundos) {
        super("Demasiados mensajes, espera un momento.");
        this.segundos = segundos;
    }

    public long segundos() {
        return segundos;
    }
}
