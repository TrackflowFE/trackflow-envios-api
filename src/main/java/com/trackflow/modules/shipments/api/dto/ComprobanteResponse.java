package com.trackflow.modules.shipments.api.dto;

import com.trackflow.modules.shipments.application.Comprobante;
import java.time.Instant;

/**
 * El comprobante tal como viaja por HTTP. Tiene los mismos seis datos que el modelo
 * y ninguno más, para que la respuesta no pueda filtrar lo que NFR-21 excluye.
 */
public record ComprobanteResponse(
        String trackingNumber,
        Instant registeredAt,
        String senderName,
        String originCity,
        String recipientName,
        String destinationCity) {

    public static ComprobanteResponse from(Comprobante comprobante) {
        return new ComprobanteResponse(
                comprobante.trackingNumber(),
                comprobante.registeredAt(),
                comprobante.senderName(),
                comprobante.originCity(),
                comprobante.recipientName(),
                comprobante.destinationCity());
    }
}
