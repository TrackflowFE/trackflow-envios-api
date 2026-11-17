package com.trackflow.modules.pickups.domain;

/**
 * La operación pedida no se puede hacer en el estado en que está la recogida.
 */
public class TransicionDeRecogidaInvalidaException extends RuntimeException {

    private final String pickupNumber;
    private final PickupStatus status;

    public TransicionDeRecogidaInvalidaException(String pickupNumber, PickupStatus status, String operacion) {
        super("La recogida " + pickupNumber + " está en estado " + status + " y no se puede " + operacion);
        this.pickupNumber = pickupNumber;
        this.status = status;
    }

    public String getPickupNumber() {
        return pickupNumber;
    }

    public PickupStatus getStatus() {
        return status;
    }
}
