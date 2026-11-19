package com.trackflow.modules.pqr.api.dto;

import com.trackflow.modules.pqr.application.VistaDePqr;
import java.time.Instant;
import java.util.List;

/** Lo que ve quien atiende la PQR: el caso completo y los datos para contactar. */
public record PqrDetalleResponse(String radicado, String tipo, String estado, String descripcion,
        String contactoNombre, String contactoEmail, String contactoTelefono, String numeroSeguimiento,
        String respuesta, String respondidoPor, Instant fechaRadicacion, Instant fechaActualizacion,
        List<HistorialPqrResponse> historial) {

    public static PqrDetalleResponse from(VistaDePqr vista) {
        var pqr = vista.pqr();
        return new PqrDetalleResponse(pqr.radicado(), pqr.tipo().name(), pqr.estado().name(), pqr.descripcion(),
                pqr.contactoNombre(), pqr.contactoEmail(), pqr.contactoTelefono(), pqr.numeroSeguimiento(),
                pqr.respuesta(), pqr.respondidoPor(), pqr.createdAt(), pqr.updatedAt(),
                vista.historial().stream().map(HistorialPqrResponse::from).toList());
    }
}
