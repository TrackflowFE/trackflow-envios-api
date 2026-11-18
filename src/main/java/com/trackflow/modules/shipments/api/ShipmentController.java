package com.trackflow.modules.shipments.api;

import com.trackflow.modules.shipments.api.dto.ComprobanteResponse;
import com.trackflow.modules.shipments.api.dto.EnvioAdmitidoResponse;
import com.trackflow.modules.shipments.api.dto.RegistrarEnvioRequest;
import com.trackflow.modules.shipments.application.AdmitirEnvio;
import com.trackflow.modules.shipments.application.ConsultarComprobante;
import com.trackflow.modules.shipments.application.EnvioSolicitado;
import jakarta.validation.Valid;
import com.trackflow.modules.shipments.api.dto.FichaEnvioResponse;
import com.trackflow.modules.shipments.application.ConsultarFichaDeEnvio;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
    private final ConsultarFichaDeEnvio consultarFicha;

    public ShipmentController(AdmitirEnvio admitirEnvio, ConsultarComprobante consultarComprobante,
            ConsultarFichaDeEnvio consultarFicha) {
        this.admitirEnvio = admitirEnvio;
        this.consultarComprobante = consultarComprobante;
        this.consultarFicha = consultarFicha;
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

    /**
     * Ficha para trabajar el envío (HU-13): el teléfono y el documento van
     * enmascarados, porque para registrar un movimiento no hace falta verlos.
     */
    @GetMapping("/{trackingNumber}/ficha")
    public ResponseEntity<FichaEnvioResponse> ficha(@PathVariable String trackingNumber) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(FichaEnvioResponse.from(consultarFicha.enmascarada(trackingNumber)));
    }

    /**
     * Datos personales completos (HU-13). Solo para administración, y cada consulta
     * queda registrada con quién la hizo y cuándo.
     */
    @GetMapping("/{trackingNumber}/ficha/completa")
    public ResponseEntity<FichaEnvioResponse> fichaCompleta(@PathVariable String trackingNumber,
            @AuthenticationPrincipal Jwt quien) {
        String actor = quien != null ? quien.getSubject() : "desconocido";
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(FichaEnvioResponse.from(consultarFicha.completa(actor, trackingNumber)));
    }
}
