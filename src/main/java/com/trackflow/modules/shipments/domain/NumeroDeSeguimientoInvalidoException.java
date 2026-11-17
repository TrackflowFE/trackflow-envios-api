package com.trackflow.modules.shipments.domain;

/**
 * El número pedido no tiene el formato de una guía TrackFlow. Es un error de la
 * petición, no un envío inexistente: la reimpresión del comprobante lo distingue
 * para responder 400 en vez de 404.
 */
public class NumeroDeSeguimientoInvalidoException extends RuntimeException {

    public NumeroDeSeguimientoInvalidoException(String trackingNumber) {
        super("Número de seguimiento inválido: " + trackingNumber);
    }
}
