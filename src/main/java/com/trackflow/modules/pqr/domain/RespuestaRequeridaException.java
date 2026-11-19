package com.trackflow.modules.pqr.domain;

/** Resolver una PQR exige decirle al solicitante qué se resolvió. */
public class RespuestaRequeridaException extends RuntimeException {

    public RespuestaRequeridaException() {
        super("Para resolver una PQR hay que escribir la respuesta (entre 10 y 2000 caracteres)");
    }
}
