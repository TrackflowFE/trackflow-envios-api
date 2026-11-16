package com.trackflow.modules.pickups.application;

import com.trackflow.modules.pickups.domain.Pickup;
import com.trackflow.modules.pickups.domain.PickupEvent;
import com.trackflow.modules.pickups.domain.PickupEventType;
import com.trackflow.modules.pickups.domain.PickupNumber;
import com.trackflow.modules.pickups.domain.PickupStatus;
import com.trackflow.modules.pickups.domain.EnvioYaVinculadoException;
import com.trackflow.modules.pickups.domain.EnvioYaEnRecorridoException;
import com.trackflow.modules.pickups.domain.EnvioVinculadoFueraDeCiudadException;
import com.trackflow.modules.pickups.domain.EnvioVinculadoNoEncontradoException;
import com.trackflow.modules.pickups.domain.RecogidaNoEncontradaException;
import com.trackflow.modules.pickups.domain.ReprogramacionSinCambiosException;
import com.trackflow.modules.pickups.domain.TransicionDeRecogidaInvalidaException;
import com.trackflow.shared.events.PickupCompletedEvent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aplica los comandos de recogida que llegan de la cola.
 *
 * Es el único que escribe las recogidas, así que es la última palabra sobre las
 * reglas: las vuelve a aplicar aunque la admisión ya las haya comprobado, porque
 * entre admitir y consumir la recogida pudo cambiar (y porque a la cola también
 * publican puntos que no pasan por la API).
 *
 * El broker entrega al menos una vez, así que un mismo comando puede llegar dos
 * veces: la segunda se descarta en lugar de aplicar el cambio dos veces.
 */
@Service
public class EjecutarComandoDeRecogida {

    private static final Logger log = LoggerFactory.getLogger(EjecutarComandoDeRecogida.class);

    private final PickupRepository recogidas;
    private final PickupEventRepository eventos;
    private final RecogidaCompletadaPublisher completada;
    private final EnviosRegistrados envios;

    public EjecutarComandoDeRecogida(PickupRepository recogidas, PickupEventRepository eventos,
            RecogidaCompletadaPublisher completada, EnviosRegistrados envios) {
        this.recogidas = recogidas;
        this.eventos = eventos;
        this.completada = completada;
        this.envios = envios;
    }

    @Transactional
    public void ejecutar(RecogidaComando comando) {
        if (eventos.existsByEventId(comando.eventId())) {
            log.info("Comando {} ya aplicado, se descarta la reentrega", comando.eventId());
            return;
        }

        try {
            switch (comando.comando()) {
                case CREAR -> crear(comando);
                case REPROGRAMAR -> reprogramar(comando);
                case CANCELAR -> cancelar(comando);
                case DESPACHAR -> despachar(comando);
                case COMPLETAR -> completar(comando);
                case VINCULAR -> vincular(comando);
            }
        } catch (RecogidaNoEncontradaException ex) {
            // Un comando puede llegar después de que la recogida ya no exista (entrega
            // tardía o mensaje de otro entorno). Reintentarlo no lo va a resolver, así
            // que se deja constancia y se confirma el mensaje en vez de enviarlo a la
            // cola de descartados.
            registrar(comando, PickupEventType.REJECTED, null, null,
                    detalleLimitado(comando.comando() + ": " + ex.getMessage()));
        }
    }

    private void crear(RecogidaComando comando) {
        PickupNumber numero = PickupNumber.of(comando.pickupNumber());
        if (recogidas.existsByNumber(numero.value())) {
            log.info("La recogida {} ya existe, se descarta el comando duplicado", numero.value());
            return;
        }

        String tracking = comando.shipmentTrackingNumber();
        String rechazo = null;
        if (tracking != null && !tracking.isBlank()) {
            try {
                validarEnvio(tracking, comando.cityId(), null);
            } catch (EnvioYaEnRecorridoException | EnvioYaVinculadoException
                    | EnvioVinculadoNoEncontradoException | EnvioVinculadoFueraDeCiudadException ex) {
                rechazo = "Guía " + tracking + " no vinculada: " + ex.getMessage();
                tracking = null;
            }
        }
        Pickup pickup = Pickup.registrar(numero,
                comando.address(),
                comando.cityId(),
                comando.contactName(),
                comando.contactPhone(),
                comando.scheduledDate(),
                comando.timeSlot(),
                comando.packageSize(),
                comando.weightKg(),
                tracking,
                comando.assignedUsername(),
                comando.actor(),
                comando.occurredAt(),
                comando.managementCodeHash());

        recogidas.save(pickup);
        registrar(comando, PickupEventType.CREATED, null, pickup.getStatus(), null);
        if (rechazo != null) {
            eventos.save(new PickupEvent(eventoGuiaId(comando.eventId()), comando.pickupNumber(),
                    PickupEventType.REJECTED, pickup.getStatus(), pickup.getStatus(), detalleLimitado(rechazo),
                    comando.actor(), comando.occurredAt()));
        }
    }

    private void reprogramar(RecogidaComando comando) {
        Pickup pickup = cargar(comando.pickupNumber());
        if (eventos.existsByEventId(comando.eventId())) {
            return;
        }
        PickupStatus anterior = pickup.getStatus();

        String fechaAnterior = pickup.getScheduledDate() + " " + pickup.getTimeSlot();
        String responsableAnterior = pickup.getAssignedUsername();

        PickupEventType tipo;
        try {
            tipo = pickup.reprogramar(comando.scheduledDate(), comando.timeSlot(),
                    comando.assignedUsername(), comando.occurredAt());
        } catch (TransicionDeRecogidaInvalidaException | ReprogramacionSinCambiosException ex) {
            rechazar(comando, pickup, ex);
            return;
        }

        recogidas.save(pickup);
        String detalle = "De " + fechaAnterior + " a " + comando.scheduledDate() + " " + comando.timeSlot();
        if (tipo == PickupEventType.ASSIGNED) {
            detalle += "; asignada a " + comando.assignedUsername() + " (antes "
                    + (responsableAnterior == null ? "sin asignar" : responsableAnterior) + ")";
        }
        registrar(comando, tipo, anterior, pickup.getStatus(), detalle);
    }

    private void cancelar(RecogidaComando comando) {
        Pickup pickup = cargar(comando.pickupNumber());
        if (eventos.existsByEventId(comando.eventId())) {
            return;
        }
        PickupStatus anterior = pickup.getStatus();

        try {
            pickup.cancelar(comando.occurredAt());
        } catch (TransicionDeRecogidaInvalidaException ex) {
            rechazar(comando, pickup, ex);
            return;
        }

        recogidas.save(pickup);
        registrar(comando, PickupEventType.CANCELLED, anterior, pickup.getStatus(), comando.reason());
    }

    private void despachar(RecogidaComando comando) {
        Pickup pickup = cargar(comando.pickupNumber());
        if (eventos.existsByEventId(comando.eventId())) {
            return;
        }
        PickupStatus anterior = pickup.getStatus();

        try {
            pickup.despachar(comando.occurredAt());
        } catch (TransicionDeRecogidaInvalidaException ex) {
            rechazar(comando, pickup, ex);
            return;
        }

        recogidas.save(pickup);
        registrar(comando, PickupEventType.DISPATCHED, anterior, pickup.getStatus(), null);
    }

    private void completar(RecogidaComando comando) {
        Pickup pickup = cargar(comando.pickupNumber());
        if (eventos.existsByEventId(comando.eventId())) {
            return;
        }
        PickupStatus anterior = pickup.getStatus();

        try {
            pickup.completar(comando.occurredAt());
        } catch (TransicionDeRecogidaInvalidaException ex) {
            rechazar(comando, pickup, ex);
            return;
        }

        recogidas.save(pickup);
        registrar(comando, PickupEventType.COMPLETED, anterior, pickup.getStatus(), null);

        // Solo hay custodia que registrar si la recogida tiene un envío. Si todavía
        // no lo tiene, el envío entra en custodia cuando se vincule.
        publicarCustodia(pickup, comando.occurredAt());
    }

    private void vincular(RecogidaComando comando) {
        Pickup pickup = cargar(comando.pickupNumber());
        if (eventos.existsByEventId(comando.eventId())) {
            return;
        }
        PickupStatus anterior = pickup.getStatus();
        boolean yaVinculado = pickup.getShipmentTrackingNumber() != null;

        try {
            validarEnvio(comando.shipmentTrackingNumber(), pickup.getCityId(), comando.pickupNumber());
        } catch (EnvioYaEnRecorridoException | EnvioYaVinculadoException
                | EnvioVinculadoNoEncontradoException | EnvioVinculadoFueraDeCiudadException ex) {
            rechazar(comando, pickup, ex);
            return;
        }
        try {
            pickup.vincularEnvio(comando.shipmentTrackingNumber(), comando.occurredAt());
        } catch (TransicionDeRecogidaInvalidaException | IllegalStateException ex) {
            rechazar(comando, pickup, ex);
            return;
        }

        recogidas.save(pickup);
        registrar(comando, PickupEventType.SHIPMENT_LINKED, anterior, pickup.getStatus(),
                "Envío " + comando.shipmentTrackingNumber());

        // Una recogida ya hecha entra en custodia en el momento de vincularse, no con
        // la fecha de la recogida: un movimiento anterior al registro del envío
        // no sería válido.
        if (pickup.getStatus() == PickupStatus.PICKED_UP && !yaVinculado) {
            publicarCustodia(pickup, comando.occurredAt());
        }
    }

    private void publicarCustodia(Pickup pickup, Instant occurredAt) {
        if (pickup.getShipmentTrackingNumber() == null) {
            return;
        }

        completada.publicar(new PickupCompletedEvent(
                pickup.getPickupNumber().value(),
                pickup.getShipmentTrackingNumber(),
                pickup.getCityId(),
                occurredAt,
                occurredAt));
    }

    private Pickup cargar(String pickupNumber) {
        return recogidas.findByNumberForUpdate(pickupNumber)
                .orElseThrow(() -> new RecogidaNoEncontradaException(pickupNumber));
    }

    private void validarEnvio(String trackingNumber, Long ciudad, String pickupNumber) {
        envios.exigir(trackingNumber, ciudad);
        if (recogidas.existsActiveByShipmentExcept(trackingNumber, pickupNumber)) {
            throw new EnvioYaVinculadoException(trackingNumber);
        }
    }

    private void rechazar(RecogidaComando comando, Pickup pickup, RuntimeException causa) {
        registrar(comando, PickupEventType.REJECTED, pickup.getStatus(), pickup.getStatus(),
                detalleLimitado(comando.comando() + ": " + causa.getMessage()));
    }

    private String detalleLimitado(String detalle) {
        return detalle.length() <= 500 ? detalle : detalle.substring(0, 500);
    }

    private String eventoGuiaId(String eventId) {
        if (eventId.length() <= 250) {
            return eventId + ":guia";
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(eventId.getBytes(StandardCharsets.UTF_8));
            return "guia:sha256:" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible", ex);
        }
    }

    private void registrar(RecogidaComando comando, PickupEventType tipo, PickupStatus anterior,
            PickupStatus nuevo, String detalle) {
        eventos.save(new PickupEvent(
                comando.eventId(),
                comando.pickupNumber(),
                tipo,
                anterior,
                nuevo,
                detalle,
                comando.actor(),
                comando.occurredAt()));
    }
}
