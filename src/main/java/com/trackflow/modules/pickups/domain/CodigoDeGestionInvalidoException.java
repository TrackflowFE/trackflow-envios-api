package com.trackflow.modules.pickups.domain;

public class CodigoDeGestionInvalidoException extends RuntimeException {
    public CodigoDeGestionInvalidoException() {
        super("Recogida o código de gestión incorrectos");
    }
}
