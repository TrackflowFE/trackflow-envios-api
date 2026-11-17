package com.trackflow.modules.pickups.domain;

/**
 * El usuario al que se quiere asignar la recogida no es un operador activo.
 */
public class OperadorDesconocidoException extends RuntimeException {

    public OperadorDesconocidoException(String username) {
        super("El usuario " + username + " no es un operador activo al que se pueda asignar una recogida");
    }
}
