package com.trackflow.modules.pickups.domain;

public class RecogidaNoEncontradaException extends RuntimeException {

    public RecogidaNoEncontradaException(String pickupNumber) {
        super("No existe una recogida con el número " + pickupNumber);
    }
}
