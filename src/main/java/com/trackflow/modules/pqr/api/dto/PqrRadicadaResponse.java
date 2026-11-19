package com.trackflow.modules.pqr.api.dto;

import com.trackflow.modules.pqr.domain.Pqr;
import java.time.Instant;

public record PqrRadicadaResponse(String radicado, String estado, String tipo, Instant fechaRadicacion,
        String mensaje) {

    private static final String MENSAJE =
            "Guarde este radicado: lo necesita, junto con su correo, para consultar el estado.";

    public static PqrRadicadaResponse from(Pqr pqr) {
        return new PqrRadicadaResponse(pqr.radicado(), pqr.estado().name(), pqr.tipo().name(), pqr.createdAt(),
                MENSAJE);
    }
}
