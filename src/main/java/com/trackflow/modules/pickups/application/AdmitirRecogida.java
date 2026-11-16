package com.trackflow.modules.pickups.application;

import com.trackflow.modules.pickups.domain.HorarioDeRecogida;
import com.trackflow.modules.pickups.domain.CodigoDeGestionInvalidoException;
import com.trackflow.modules.pickups.domain.EnvioYaVinculadoException;
import com.trackflow.modules.pickups.domain.PackageSize;
import com.trackflow.modules.pickups.domain.Pickup;
import com.trackflow.modules.pickups.domain.PickupNumber;
import com.trackflow.modules.pickups.domain.PickupStatus;
import com.trackflow.modules.pickups.domain.RecogidaNoEncontradaException;
import com.trackflow.modules.pickups.domain.ReprogramacionSinCambiosException;
import com.trackflow.modules.pickups.domain.TransicionDeRecogidaInvalidaException;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Admite los comandos de recogida y los publica.
 *
 * Aquí se decide si una petición es admisible, y por eso es donde se resuelven las
 * ciudades, el horario de atención y el envío al que se vincula: una fecha que no se
 * puede atender se rechaza de inmediato con su motivo, en lugar de acabar en la cola
 * de descartados donde nadie lo va a leer.
 *
 * El comando no se aplica aquí: lo aplica el consumidor de la cola. Entre una cosa y
 * otra el estado puede cambiar, así que el consumidor vuelve a validar antes de
 * escribir; esta validación es para dar una respuesta útil, no la última palabra.
 */
@Service
public class AdmitirRecogida {

    /** Lo que hay que saber para abrir una recogida. */
    public record Solicitud(
            String address,
            Long cityId,
            String contactName,
            String contactPhone,
            LocalDate scheduledDate,
            String timeSlot,
            PackageSize packageSize,
            BigDecimal weightKg,
            String shipmentTrackingNumber,
            String assignedUsername,
            String actor) {
    }

    private final PickupNumberGenerator numeros;
    private final RecogidaComandoPublisher publisher;
    private final CatalogoDeCiudades ciudades;
    private final HorarioDeRecogida horario;
    private final OperadoresDisponibles operadores;
    private final EnviosRegistrados envios;
    private final PickupRepository recogidas;
    private final Clock clock;
    private final CodigosDeGestion codigos;

    public AdmitirRecogida(PickupNumberGenerator numeros, RecogidaComandoPublisher publisher,
            CatalogoDeCiudades ciudades, HorarioDeRecogida horario, OperadoresDisponibles operadores,
            EnviosRegistrados envios, PickupRepository recogidas, Clock clock, CodigosDeGestion codigos) {
        this.numeros = numeros;
        this.publisher = publisher;
        this.ciudades = ciudades;
        this.horario = horario;
        this.operadores = operadores;
        this.envios = envios;
        this.recogidas = recogidas;
        this.clock = clock;
        this.codigos = codigos;
    }

    /** Admite el registro de una recogida nueva y encola la solicitud. */
    public RecogidaSolicitada crear(Solicitud solicitud) {
        Instant ahora = clock.instant();

        // La ciudad se resuelve antes de encolar: una ciudad inexistente se rechaza
        // de inmediato, no dentro de la cola.
        ciudades.exigir(solicitud.cityId());
        horario.exigir(solicitud.scheduledDate(), solicitud.timeSlot(), ahora);

        if (tieneTexto(solicitud.assignedUsername())) {
            operadores.exigir(solicitud.assignedUsername());
        }
        if (tieneTexto(solicitud.shipmentTrackingNumber())) {
            validarEnvio(solicitud.shipmentTrackingNumber(), solicitud.cityId(), null);
        }

        PickupNumber numero = numeros.next();
        String codigo = codigos.generar();

        RecogidaComando comando = new RecogidaComando(
                UUID.randomUUID().toString(),
                RecogidaComando.Tipo.CREAR,
                numero.value(),
                solicitud.actor(),
                ahora,
                solicitud.address(),
                solicitud.cityId(),
                solicitud.contactName(),
                solicitud.contactPhone(),
                solicitud.scheduledDate(),
                solicitud.timeSlot(),
                solicitud.packageSize(),
                solicitud.weightKg(),
                solicitud.shipmentTrackingNumber(),
                solicitud.assignedUsername(),
                null,
                codigos.hash(codigo));

        publisher.publicar(comando);

        return new RecogidaSolicitada(comando.eventId(), numero.value(), PickupStatus.REQUESTED.name(), ahora, codigo);
    }

    /** Admite cambiar la fecha o la franja de una recogida, y asignarla a un operador. */
    public void reprogramar(String pickupNumber, LocalDate fecha, String franja, String responsable, String actor) {
        Pickup pickup = exigir(pickupNumber);
        if (!pickup.getStatus().admiteReprogramacion()) {
            throw new TransicionDeRecogidaInvalidaException(pickupNumber, pickup.getStatus(), "reprogramar");
        }

        if (Objects.equals(fecha, pickup.getScheduledDate()) && Objects.equals(franja, pickup.getTimeSlot())
                && (!tieneTexto(responsable) || responsable.equals(pickup.getAssignedUsername()))) {
            throw new ReprogramacionSinCambiosException();
        }

        Instant ahora = clock.instant();
        horario.exigir(fecha, franja, ahora);
        if (tieneTexto(responsable)) {
            operadores.exigir(responsable);
        }

        publisher.publicar(new RecogidaComando(
                UUID.randomUUID().toString(),
                RecogidaComando.Tipo.REPROGRAMAR,
                pickupNumber,
                actor,
                ahora,
                null, null, null, null,
                fecha,
                franja,
                null, null,
                null,
                responsable,
                null));
    }

    /** Admite cancelar una recogida que todavía no salió a recoger. */
    public void cancelar(String pickupNumber, String motivo, String actor) {
        Pickup pickup = exigir(pickupNumber);
        if (!pickup.getStatus().admiteCancelacion()) {
            throw new TransicionDeRecogidaInvalidaException(pickupNumber, pickup.getStatus(), "cancelar");
        }

        publisher.publicar(new RecogidaComando(
                UUID.randomUUID().toString(),
                RecogidaComando.Tipo.CANCELAR,
                pickupNumber,
                actor,
                clock.instant(),
                null, null, null, null, null, null, null, null, null, null,
                motivo));
    }

    /** Admite que el operador salga hacia la dirección del remitente. */
    public void despachar(String pickupNumber, String actor) {
        Pickup pickup = exigir(pickupNumber);
        if (!pickup.getStatus().admiteDespacho()) {
            throw new TransicionDeRecogidaInvalidaException(pickupNumber, pickup.getStatus(), "despachar");
        }

        publisher.publicar(comandoSimple(RecogidaComando.Tipo.DESPACHAR, pickupNumber, actor));
    }

    /** Admite registrar que el paquete ya fue recogido. */
    public void completar(String pickupNumber, String actor) {
        Pickup pickup = exigir(pickupNumber);
        if (!pickup.getStatus().admiteCompletado()) {
            throw new TransicionDeRecogidaInvalidaException(pickupNumber, pickup.getStatus(), "completar");
        }

        publisher.publicar(comandoSimple(RecogidaComando.Tipo.COMPLETAR, pickupNumber, actor));
    }

    /**
     * Admite vincular un envío a una recogida.
     *
     * El envío tiene que salir de la ciudad donde se recoge: si no, la dirección y la
     * guía no pueden ser las mismas.
     */
    public void vincular(String pickupNumber, String trackingNumber, String actor) {
        Pickup pickup = exigir(pickupNumber);
        if (!pickup.getStatus().admiteVinculacion()) {
            throw new TransicionDeRecogidaInvalidaException(pickupNumber, pickup.getStatus(), "vincular un envío");
        }
        if (pickup.getShipmentTrackingNumber() != null
                && !pickup.getShipmentTrackingNumber().equals(trackingNumber)) {
            throw new IllegalStateException("La recogida ya está vinculada a otro envío");
        }

        validarEnvio(trackingNumber, pickup.getCityId(), pickupNumber);

        publisher.publicar(new RecogidaComando(
                UUID.randomUUID().toString(),
                RecogidaComando.Tipo.VINCULAR,
                pickupNumber,
                actor,
                clock.instant(),
                null, null, null, null, null, null, null, null,
                trackingNumber,
                null,
                null));
    }

    private RecogidaComando comandoSimple(RecogidaComando.Tipo tipo, String pickupNumber, String actor) {
        return new RecogidaComando(
                UUID.randomUUID().toString(),
                tipo,
                pickupNumber,
                actor,
                clock.instant(),
                null, null, null, null, null, null, null, null, null, null,
                null);
    }

    private Pickup exigir(String pickupNumber) {
        return recogidas.findByNumber(pickupNumber)
                .orElseThrow(() -> new RecogidaNoEncontradaException(pickupNumber));
    }

    public void reprogramarComoRemitente(String pickupNumber, String codigo, LocalDate fecha, String franja) {
        exigirCodigo(pickupNumber, codigo);
        reprogramar(pickupNumber, fecha, franja, null, "remitente");
    }

    public void cancelarComoRemitente(String pickupNumber, String codigo, String motivo) {
        exigirCodigo(pickupNumber, codigo);
        cancelar(pickupNumber, motivo, "remitente");
    }

    private void exigirCodigo(String pickupNumber, String codigo) {
        Pickup pickup = recogidas.findByNumber(pickupNumber).orElse(null);
        boolean valido = codigos.coincide(codigo, pickup == null ? null : pickup.getManagementCodeHash());
        if (pickup == null || !pickup.admiteAutogestion() || !valido) {
            throw new CodigoDeGestionInvalidoException();
        }
    }

    private void validarEnvio(String trackingNumber, Long ciudad, String pickupNumber) {
        envios.exigir(trackingNumber, ciudad);
        if (recogidas.existsActiveByShipmentExcept(trackingNumber, pickupNumber)) {
            throw new EnvioYaVinculadoException(trackingNumber);
        }
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
