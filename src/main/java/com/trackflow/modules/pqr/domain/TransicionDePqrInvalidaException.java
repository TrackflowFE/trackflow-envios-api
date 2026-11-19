package com.trackflow.modules.pqr.domain;

/** La PQR no puede pasar del estado en que está al que se pidió. */
public class TransicionDePqrInvalidaException extends RuntimeException {

    public TransicionDePqrInvalidaException(String radicado, EstadoPqr actual, EstadoPqr pedido) {
        super("La PQR " + radicado + " está " + actual + " y no puede pasar a " + pedido);
    }
}
