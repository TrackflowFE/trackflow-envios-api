package com.trackflow.modules.pqr.api.dto;

import com.trackflow.modules.pqr.domain.PqrEvento;
import java.time.Instant;

public record HistorialPqrResponse(String estado, Instant fecha) {

    public static HistorialPqrResponse from(PqrEvento evento) {
        return new HistorialPqrResponse(evento.estadoNuevo().name(), evento.occurredAt());
    }
}
