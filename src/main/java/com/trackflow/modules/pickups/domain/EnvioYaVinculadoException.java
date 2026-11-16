package com.trackflow.modules.pickups.domain;

public class EnvioYaVinculadoException extends RuntimeException {
    public EnvioYaVinculadoException(String trackingNumber) {
        super("El envío " + trackingNumber + " ya está vinculado a otra recogida activa");
    }
}
