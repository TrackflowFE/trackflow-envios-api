package com.trackflow.modules.shipments.application;

import com.trackflow.modules.shipments.domain.Party;
import com.trackflow.modules.shipments.domain.Shipment;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import com.trackflow.shared.seguridad.Enmascarado;
import java.time.Instant;

/**
 * Ficha de un envío para quien lo atiende (HU-13).
 *
 * Se construye de dos maneras: enmascarada, que es la que ve el operador en su
 * trabajo diario, y completa, reservada al administrador y cuyo uso queda
 * registrado. El enmascaramiento ocurre aquí, al construir la ficha, no en la
 * capa web: así ninguna respuesta puede saltárselo por descuido.
 */
public record FichaDeEnvio(
        String trackingNumber,
        String status,
        Instant registeredAt,
        String description,
        Persona sender,
        Persona recipient,
        boolean datosCompletos) {

    public record Persona(String name, String documentType, String documentNumber, String phone, String address,
            String city) {
    }

    public static FichaDeEnvio enmascarada(Shipment shipment, CatalogoDeCiudades ciudades) {
        return new FichaDeEnvio(
                shipment.getTrackingNumber().value(),
                shipment.getStatus().name(),
                shipment.getRegisteredAt(),
                shipment.getDescription(),
                persona(shipment.getSender(), ciudades, false),
                persona(shipment.getRecipient(), ciudades, false),
                false);
    }

    public static FichaDeEnvio completa(Shipment shipment, CatalogoDeCiudades ciudades) {
        return new FichaDeEnvio(
                shipment.getTrackingNumber().value(),
                shipment.getStatus().name(),
                shipment.getRegisteredAt(),
                shipment.getDescription(),
                persona(shipment.getSender(), ciudades, true),
                persona(shipment.getRecipient(), ciudades, true),
                true);
    }

    private static Persona persona(Party parte, CatalogoDeCiudades ciudades, boolean completa) {
        String ciudad = ciudades.porId(parte.getCityId()).map(c -> c.nombre()).orElse(null);
        if (completa) {
            return new Persona(parte.getFullName(), parte.getDocumentType().name(), parte.getDocumentNumber(),
                    parte.getPhone(), parte.getAddress(), ciudad);
        }
        return new Persona(parte.getFullName(), parte.getDocumentType().name(),
                Enmascarado.documento(parte.getDocumentNumber()),
                Enmascarado.telefono(parte.getPhone()),
                Enmascarado.direccion(parte.getAddress()),
                ciudad);
    }
}
