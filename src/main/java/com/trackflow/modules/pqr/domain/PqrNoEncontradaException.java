package com.trackflow.modules.pqr.domain;

/**
 * No hay una PQR con ese radicado, o el correo no es el registrado.
 *
 * Es una sola excepción para las dos causas, a propósito: si fueran distintas, la
 * respuesta delataría qué radicados existen.
 */
public class PqrNoEncontradaException extends RuntimeException {

    public PqrNoEncontradaException() {
        super("No encontramos una PQR con esos datos");
    }
}
