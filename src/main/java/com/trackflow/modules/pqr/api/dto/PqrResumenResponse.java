package com.trackflow.modules.pqr.api.dto;

import com.trackflow.modules.pqr.domain.Pqr;
import java.time.Instant;

/** Una fila de la bandeja: lo justo para escoger cuál abrir. */
public record PqrResumenResponse(String radicado, String tipo, String estado, String contactoNombre,
        Instant fechaRadicacion) {

    public static PqrResumenResponse from(Pqr pqr) {
        return new PqrResumenResponse(pqr.radicado(), pqr.tipo().name(), pqr.estado().name(), pqr.contactoNombre(),
                pqr.createdAt());
    }
}
