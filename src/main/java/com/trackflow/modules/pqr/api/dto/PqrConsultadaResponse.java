package com.trackflow.modules.pqr.api.dto;

import com.trackflow.modules.pqr.application.VistaDePqr;
import java.time.Instant;
import java.util.List;

/**
 * Lo que ve el solicitante. No lleva teléfono, correo ni quién respondió, y la
 * respuesta solo aparece cuando la PQR ya está resuelta o cerrada.
 */
public record PqrConsultadaResponse(String radicado, String tipo, String estado, Instant fechaRadicacion,
        Instant fechaActualizacion, String respuesta, List<HistorialPqrResponse> historial) {

    public static PqrConsultadaResponse from(VistaDePqr vista) {
        var pqr = vista.pqr();
        return new PqrConsultadaResponse(pqr.radicado(), pqr.tipo().name(), pqr.estado().name(), pqr.createdAt(),
                pqr.updatedAt(), pqr.estado().muestraRespuesta() ? pqr.respuesta() : null,
                vista.historial().stream().map(HistorialPqrResponse::from).toList());
    }
}
