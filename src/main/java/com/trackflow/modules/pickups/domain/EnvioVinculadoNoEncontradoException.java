package com.trackflow.modules.pickups.domain;

/**
 * El envío al que se quiere ligar la recogida no existe.
 */
public class EnvioVinculadoNoEncontradoException extends RuntimeException {

    public EnvioVinculadoNoEncontradoException(String trackingNumber) {
        super("No existe un envío con el número de seguimiento " + trackingNumber);
    }
}
