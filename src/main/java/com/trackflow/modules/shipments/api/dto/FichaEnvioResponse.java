package com.trackflow.modules.shipments.api.dto;

import com.trackflow.modules.shipments.application.FichaDeEnvio;
import java.time.Instant;

/**
 * La ficha tal como viaja por HTTP. El indicador datosCompletos permite a la
 * interfaz avisar de que lo que se está viendo quedó registrado.
 */
public record FichaEnvioResponse(
        String trackingNumber,
        String status,
        Instant registeredAt,
        String description,
        PersonaResponse sender,
        PersonaResponse recipient,
        boolean datosCompletos) {

    public record PersonaResponse(String name, String documentType, String documentNumber, String phone,
            String address, String city) {
    }

    public static FichaEnvioResponse from(FichaDeEnvio ficha) {
        return new FichaEnvioResponse(
                ficha.trackingNumber(),
                ficha.status(),
                ficha.registeredAt(),
                ficha.description(),
                persona(ficha.sender()),
                persona(ficha.recipient()),
                ficha.datosCompletos());
    }

    private static PersonaResponse persona(FichaDeEnvio.Persona p) {
        return new PersonaResponse(p.name(), p.documentType(), p.documentNumber(), p.phone(), p.address(), p.city());
    }
}
