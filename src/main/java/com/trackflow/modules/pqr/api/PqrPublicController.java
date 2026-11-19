package com.trackflow.modules.pqr.api;

import com.trackflow.modules.pqr.api.dto.ConsultaPqrRequest;
import com.trackflow.modules.pqr.api.dto.PqrConsultadaResponse;
import com.trackflow.modules.pqr.api.dto.PqrRadicadaResponse;
import com.trackflow.modules.pqr.api.dto.RadicarPqrRequest;
import com.trackflow.modules.pqr.application.ConsultarPqr;
import com.trackflow.modules.pqr.application.RadicarPqr;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lo que hace el solicitante, sin cuenta. La consulta es un POST y no un GET para que
 * el correo no viaje en la URL, donde queda en los registros.
 */
@RestController
@RequestMapping("/api/pqr")
public class PqrPublicController {

    private final RadicarPqr radicar;
    private final ConsultarPqr consultar;

    public PqrPublicController(RadicarPqr radicar, ConsultarPqr consultar) {
        this.radicar = radicar;
        this.consultar = consultar;
    }

    @PostMapping
    public ResponseEntity<PqrRadicadaResponse> radicar(@Valid @RequestBody RadicarPqrRequest request) {
        var pqr = radicar.radicar(new RadicarPqr.Solicitud(request.tipo(), request.descripcion(),
                request.contactoNombre(), request.contactoEmail(), request.contactoTelefono(),
                request.numeroSeguimiento(), request.aceptaTratamientoDatos()));
        return ResponseEntity.status(HttpStatus.CREATED).body(PqrRadicadaResponse.from(pqr));
    }

    @PostMapping("/consulta")
    public PqrConsultadaResponse consultar(@Valid @RequestBody ConsultaPqrRequest request) {
        return PqrConsultadaResponse.from(consultar.delSolicitante(request.radicado(), request.email()));
    }
}
