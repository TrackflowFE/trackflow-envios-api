package com.trackflow.modules.shipments.api;

import com.trackflow.modules.shipments.api.dto.ComprobanteResponse;
import com.trackflow.modules.shipments.api.dto.EnvioAdmitidoResponse;
import com.trackflow.modules.shipments.api.dto.RegistrarEnvioRequest;
import com.trackflow.modules.shipments.application.AdmitirEnvio;
import com.trackflow.modules.shipments.application.ConsultarComprobante;
import com.trackflow.modules.shipments.application.EnvioSolicitado;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST. Valida y publica la solicitud en el broker; el envío
 * lo registra el consumidor de la cola. El número de seguimiento se devuelve ya,
 * porque HU-01 exige entregárselo al remitente en el momento.
 */
@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final AdmitirEnvio admitirEnvio;
    private final ConsultarComprobante consultarComprobante;

    public ShipmentController(AdmitirEnvio admitirEnvio, ConsultarComprobante consultarComprobante) {
        this.admitirEnvio = admitirEnvio;
        this.consultarComprobante = consultarComprobante;
    }

    @PostMapping
    public ResponseEntity<EnvioAdmitidoResponse> admitir(@Valid @RequestBody RegistrarEnvioRequest request) {
        EnvioSolicitado solicitud = admitirEnvio.ejecutar(new AdmitirEnvio.Command(
                request.sender().toDatos(),
                request.recipient().toDatos(),
                request.description()));

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(EnvioAdmitidoResponse.from(solicitud));
    }

    /**
     * Reimpresión del comprobante (HU-14). Lleva nombres de personas, así que exige
     * sesión de operador y no se cachea.
     */
    @GetMapping("/{trackingNumber}/receipt")
    public ResponseEntity<ComprobanteResponse> comprobante(@PathVariable String trackingNumber) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ComprobanteResponse.from(consultarComprobante.porNumero(trackingNumber)));
    }
}
