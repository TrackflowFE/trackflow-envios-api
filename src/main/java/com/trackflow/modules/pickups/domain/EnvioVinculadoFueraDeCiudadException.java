package com.trackflow.modules.pickups.domain;

/**
 * El envío al que se quiere ligar la recogida está en otra ciudad.
 *
 * No tiene sentido recoger en una ciudad un paquete que viaja desde otra: o la
 * dirección está mal, o la guía no es la que se cree.
 */
public class EnvioVinculadoFueraDeCiudadException extends RuntimeException {

    public EnvioVinculadoFueraDeCiudadException(String trackingNumber, Long ciudadRecogida) {
        super("El envío " + trackingNumber + " no se puede recoger en la ciudad " + ciudadRecogida
                + ": su origen está en otra ciudad");
    }
}
