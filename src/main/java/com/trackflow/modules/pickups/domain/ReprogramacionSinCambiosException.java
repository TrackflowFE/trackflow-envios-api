package com.trackflow.modules.pickups.domain;

public class ReprogramacionSinCambiosException extends RuntimeException {
    public ReprogramacionSinCambiosException() {
        super("La reprogramación no modifica la recogida");
    }
}
