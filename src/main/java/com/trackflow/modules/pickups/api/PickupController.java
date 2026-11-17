package com.trackflow.modules.pickups.api;

import com.trackflow.modules.pickups.api.dto.CancelarRecogidaRequest;
import com.trackflow.modules.pickups.api.dto.HorarioRecogidaResponse;
import com.trackflow.modules.pickups.api.dto.RecogidaAdmitidaResponse;
import com.trackflow.modules.pickups.api.dto.RecogidaResumenResponse;
import com.trackflow.modules.pickups.api.dto.RecogidaResponse;
import com.trackflow.modules.pickups.api.dto.ReprogramarRecogidaRequest;
import com.trackflow.modules.pickups.api.dto.SolicitarRecogidaRequest;
import com.trackflow.modules.pickups.api.dto.VincularEnvioRequest;
import com.trackflow.modules.pickups.application.AdmitirRecogida;
import com.trackflow.modules.pickups.application.ConsultarRecogidas;
import com.trackflow.modules.pickups.application.OperadoresDisponibles;
import com.trackflow.modules.pickups.application.RecogidaSolicitada;
import com.trackflow.modules.pickups.application.VistaDeRecogida;
import com.trackflow.modules.pickups.domain.HorarioDeRecogida;
import com.trackflow.modules.pickups.domain.PickupStatus;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST de las recogidas a domicilio.
 *
 * Valida y publica el comando; el cambio lo aplica el consumidor de la cola. El
 * número de la recogida se devuelve ya, porque quien la pide necesita enterarse
 * en el momento de que quedó registrada.
 *
 * Quien consulta sin tener la recogida asignada recibe el resto de los datos, pero
 * no la dirección ni el contacto: eso lo decide la aplicación, no el controlador.
 */
@RestController
@RequestMapping("/api/pickups")
public class PickupController {

    /** Identificador de las acciones hechas sin sesión abierta (entorno local). */
    static final String SIN_SESION = "sistema";

    private final AdmitirRecogida admitir;
    private final ConsultarRecogidas consultar;
    private final OperadoresDisponibles operadores;
    private final HorarioDeRecogida horario;

    public PickupController(AdmitirRecogida admitir, ConsultarRecogidas consultar, OperadoresDisponibles operadores,
            HorarioDeRecogida horario) {
        this.admitir = admitir;
        this.consultar = consultar;
        this.operadores = operadores;
        this.horario = horario;
    }

    /** Cuándo se puede recoger: público, porque es el horario del servicio. */
    @GetMapping("/operating-hours")
    public HorarioRecogidaResponse horario() {
        return HorarioRecogidaResponse.from(horario);
    }

    /** Operadores a los que se puede asignar una recogida. */
    @GetMapping("/assignees")
    public List<String> asignables() {
        return operadores.activos();
    }

    @PostMapping
    public ResponseEntity<RecogidaAdmitidaResponse> crear(@Valid @RequestBody SolicitarRecogidaRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        RecogidaSolicitada solicitud = admitir.crear(new AdmitirRecogida.Solicitud(
                request.address(),
                request.cityId(),
                request.contactName(),
                request.contactPhone(),
                request.scheduledDate(),
                request.timeSlot(),
                request.packageSize(),
                request.weightKg(),
                request.shipmentTrackingNumber(),
                null,
                actorDe(jwt)));

        return ResponseEntity.status(HttpStatus.ACCEPTED).cacheControl(org.springframework.http.CacheControl.noStore())
                .body(RecogidaAdmitidaResponse.from(solicitud));
    }

    @GetMapping("/{pickupNumber}")
    public RecogidaResponse una(@PathVariable String pickupNumber, @AuthenticationPrincipal Jwt jwt) {
        VistaDeRecogida vista = consultar.una(pickupNumber, jwt == null ? null : jwt.getSubject());
        return RecogidaResponse.from(vista);
    }

    @GetMapping
    public List<RecogidaResumenResponse> listar(@RequestParam(required = false) PickupStatus status,
            @RequestParam(required = false) String assignedTo,
            @AuthenticationPrincipal Jwt jwt) {
        if (assignedTo != null && !"me".equals(assignedTo)) {
            throw new IllegalArgumentException("assignedTo solo admite 'me'");
        }
        return consultar.listar(status, jwt == null ? null : jwt.getSubject(), "me".equals(assignedTo)).stream()
                .map(RecogidaResumenResponse::from)
                .toList();
    }

    /** Cambia la fecha o la franja de la recogida y, si se indica, la asigna. */
    @PutMapping("/{pickupNumber}/schedule")
    public ResponseEntity<Void> reprogramar(@PathVariable String pickupNumber,
            @Valid @RequestBody ReprogramarRecogidaRequest request, @AuthenticationPrincipal Jwt jwt) {
        admitir.reprogramar(pickupNumber, request.scheduledDate(), request.timeSlot(), request.assignedUsername(),
                actorDe(jwt));

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/{pickupNumber}/cancel")
    public ResponseEntity<Void> cancelar(@PathVariable String pickupNumber,
            @Valid @RequestBody(required = false) CancelarRecogidaRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String motivo = request == null ? null : request.reason();
        admitir.cancelar(pickupNumber, motivo, actorDe(jwt));

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/{pickupNumber}/dispatch")
    public ResponseEntity<Void> despachar(@PathVariable String pickupNumber, @AuthenticationPrincipal Jwt jwt) {
        admitir.despachar(pickupNumber, actorDe(jwt));

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/{pickupNumber}/complete")
    public ResponseEntity<Void> completar(@PathVariable String pickupNumber, @AuthenticationPrincipal Jwt jwt) {
        admitir.completar(pickupNumber, actorDe(jwt));

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    /** Vincula la recogida con un envío: el paquete recogido entra en custodia. */
    @PutMapping("/{pickupNumber}/shipment")
    public ResponseEntity<Void> vincular(@PathVariable String pickupNumber,
            @Valid @RequestBody VincularEnvioRequest request, @AuthenticationPrincipal Jwt jwt) {
        admitir.vincular(pickupNumber, request.trackingNumber(), actorDe(jwt));

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    /** El usuario que hace el cambio; sin sesión abierta se anota como "sistema". */
    private static String actorDe(Jwt jwt) {
        return jwt == null ? SIN_SESION : jwt.getSubject();
    }
}
