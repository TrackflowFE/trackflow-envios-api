package com.trackflow.modules.pickups.domain;

public class EnvioYaEnRecorridoException extends RuntimeException {
    public EnvioYaEnRecorridoException(String trackingNumber) {
        super("El envío " + trackingNumber + " ya no está registrado sin movimientos");
    }
}
