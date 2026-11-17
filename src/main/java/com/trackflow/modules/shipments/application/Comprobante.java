package com.trackflow.modules.shipments.application;

import com.trackflow.modules.shipments.domain.Shipment;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import com.trackflow.shared.geografia.Ciudad;
import java.time.Instant;

/**
 * El comprobante que se entrega al remitente (HU-14).
 *
 * Es la única fuente del papel: la impresión inmediata al registrar y la
 * reimpresión posterior salen del mismo tipo, así que no pueden diferir.
 *
 * Solo lleva lo que NFR-21 permite mostrar: número, fecha, nombres y ciudades.
 * No tiene teléfonos, documentos, direcciones ni la descripción del paquete, y
 * esa ausencia es deliberada, no un olvido.
 */
public record Comprobante(
        String trackingNumber,
        Instant registeredAt,
        String senderName,
        String originCity,
        String recipientName,
        String destinationCity) {

    /**
     * Comprobante de una solicitud recién admitida: las ciudades ya vienen resueltas
     * desde el catálogo, así que no hace falta consultarlo otra vez.
     */
    public static Comprobante de(EnvioSolicitado solicitud) {
        return new Comprobante(
                solicitud.trackingNumber(),
                solicitud.solicitadoEn(),
                solicitud.remitente().getFullName(),
                solicitud.ciudadOrigen().etiqueta(),
                solicitud.destinatario().getFullName(),
                solicitud.ciudadDestino().etiqueta());
    }

    /**
     * Comprobante de un envío ya guardado. Las ciudades se buscan en el catálogo por
     * su identificador; si alguna no aparece, el envío apunta a un dato inconsistente
     * y no hay comprobante válido que emitir.
     */
    public static Comprobante de(Shipment shipment, CatalogoDeCiudades ciudades) {
        return new Comprobante(
                shipment.getTrackingNumber().value(),
                shipment.getRegisteredAt(),
                shipment.getSender().getFullName(),
                etiqueta(ciudades, shipment.getSender().getCityId()),
                shipment.getRecipient().getFullName(),
                etiqueta(ciudades, shipment.getRecipient().getCityId()));
    }

    private static String etiqueta(CatalogoDeCiudades ciudades, Long cityId) {
        return ciudades.porId(cityId)
                .map(Ciudad::etiqueta)
                .orElseThrow(() -> new IllegalStateException(
                        "La ciudad " + cityId + " de un envío no está en el catálogo"));
    }
}
