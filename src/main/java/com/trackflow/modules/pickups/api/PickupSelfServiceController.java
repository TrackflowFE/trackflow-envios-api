package com.trackflow.modules.pickups.api;

import com.trackflow.modules.pickups.api.dto.CancelarRecogidaRequest;
import com.trackflow.modules.pickups.api.dto.RecogidaAdmitidaResponse;
import com.trackflow.modules.pickups.api.dto.RecogidaResponse;
import com.trackflow.modules.pickups.api.dto.SolicitarRecogidaRequest;
import com.trackflow.modules.pickups.application.AdmitirRecogida;
import com.trackflow.modules.pickups.application.ConsultarRecogidas;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pickups/self-service")
public class PickupSelfServiceController {
    private final AdmitirRecogida admitir;
    private final ConsultarRecogidas consultar;

    public PickupSelfServiceController(AdmitirRecogida admitir, ConsultarRecogidas consultar) {
        this.admitir = admitir;
        this.consultar = consultar;
    }

    public record HorarioRequest(@NotNull LocalDate scheduledDate,
            @NotBlank @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d-([01]\\d|2[0-3]):[0-5]\\d$") String timeSlot) {
    }

    @PostMapping
    public ResponseEntity<RecogidaAdmitidaResponse> crear(@Valid @RequestBody SolicitarRecogidaRequest request) {
        if (request.shipmentTrackingNumber() != null) {
            throw new IllegalArgumentException("Solo un operador puede vincular un envío a una recogida");
        }
        var solicitud = admitir.crear(new AdmitirRecogida.Solicitud(request.address(), request.cityId(),
                request.contactName(), request.contactPhone(), request.scheduledDate(), request.timeSlot(),
                request.packageSize(), request.weightKg(), request.shipmentTrackingNumber(), null, "remitente"));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(RecogidaAdmitidaResponse.from(solicitud));
    }

    @GetMapping("/{pickupNumber}")
    public RecogidaResponse una(@PathVariable String pickupNumber,
            @RequestHeader(value = "X-Codigo-Gestion", required = false) String codigo) {
        return RecogidaResponse.from(consultar.delRemitente(pickupNumber, codigo));
    }

    @PutMapping("/{pickupNumber}/schedule")
    public ResponseEntity<Void> reprogramar(@PathVariable String pickupNumber,
            @RequestHeader(value = "X-Codigo-Gestion", required = false) String codigo,
            @Valid @RequestBody HorarioRequest request) {
        admitir.reprogramarComoRemitente(pickupNumber, codigo, request.scheduledDate(), request.timeSlot());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{pickupNumber}/cancel")
    public ResponseEntity<Void> cancelar(@PathVariable String pickupNumber,
            @RequestHeader(value = "X-Codigo-Gestion", required = false) String codigo,
            @Valid @RequestBody(required = false) CancelarRecogidaRequest request) {
        admitir.cancelarComoRemitente(pickupNumber, codigo, request == null ? null : request.reason());
        return ResponseEntity.accepted().build();
    }
}
