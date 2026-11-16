package com.trackflow.modules.pickups.application;

import com.trackflow.modules.pickups.domain.Pickup;
import com.trackflow.modules.pickups.domain.PickupEvent;
import com.trackflow.modules.pickups.domain.PickupEventType;
import com.trackflow.modules.pickups.domain.PickupNumber;
import com.trackflow.modules.pickups.domain.PickupStatus;
import com.trackflow.modules.pickups.domain.RecogidaNoEncontradaException;
import com.trackflow.shared.events.PickupCompletedEvent;
import java.time.Instant;
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

    public EjecutarComandoDeRecogida(PickupRepository recogidas, PickupEventRepository eventos,
            RecogidaCompletadaPublisher completada) {
        this.recogidas = recogidas;
        this.eventos = eventos;
        this.completada = completada;
    }

    @Transactional
    public void ejecutar(RecogidaComando comando) {
        if (eventos.existsByEventId(comando.eventId())) {
            log.info("Comando {} ya aplicado, se descarta la reentrega", comando.eventId());
            return;
        }

        switch (comando.comando()) {
            case CREAR -> crear(comando);
            case REPROGRAMAR -> reprogramar(comando);
            case CANCELAR -> cancelar(comando);
            case DESPACHAR -> despachar(comando);
            case COMPLETAR -> completar(comando);
            case VINCULAR -> vincular(comando);
        }
    }

    private void crear(RecogidaComando comando) {
        PickupNumber numero = PickupNumber.of(comando.pickupNumber());
        if (recogidas.existsByNumber(numero.value())) {
            log.info("La recogida {} ya existe, se descarta el comando duplicado", numero.value());
            return;
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
                comando.shipmentTrackingNumber(),
                comando.assignedUsername(),
                comando.actor(),
                comando.occurredAt());

        recogidas.save(pickup);
        registrar(comando, PickupEventType.CREATED, null, pickup.getStatus(), null);
    }

    private void reprogramar(RecogidaComando comando) {
        Pickup pickup = cargar(comando.pickupNumber());
        if (eventos.existsByEventId(comando.eventId())) {
            return;
        }
        PickupStatus anterior = pickup.getStatus();

        PickupEventType tipo = pickup.reprogramar(comando.scheduledDate(), comando.timeSlot(),
                comando.assignedUsername(), comando.occurredAt());

        recogidas.save(pickup);
        registrar(comando, tipo, anterior, pickup.getStatus(),
                "Fecha " + comando.scheduledDate() + ", franja " + comando.timeSlot());
    }

    private void cancelar(RecogidaComando comando) {
        Pickup pickup = cargar(comando.pickupNumber());
        if (eventos.existsByEventId(comando.eventId())) {
            return;
        }
        PickupStatus anterior = pickup.getStatus();

        pickup.cancelar(comando.occurredAt());

        recogidas.save(pickup);
        registrar(comando, PickupEventType.CANCELLED, anterior, pickup.getStatus(), comando.reason());
    }

    private void despachar(RecogidaComando comando) {
        Pickup pickup = cargar(comando.pickupNumber());
        if (eventos.existsByEventId(comando.eventId())) {
            return;
        }
        PickupStatus anterior = pickup.getStatus();

        pickup.despachar(comando.occurredAt());

        recogidas.save(pickup);
        registrar(comando, PickupEventType.DISPATCHED, anterior, pickup.getStatus(), null);
    }

    private void completar(RecogidaComando comando) {
        Pickup pickup = cargar(comando.pickupNumber());
        if (eventos.existsByEventId(comando.eventId())) {
            return;
        }
        PickupStatus anterior = pickup.getStatus();

        pickup.completar(comando.occurredAt());

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

        pickup.vincularEnvio(comando.shipmentTrackingNumber(), comando.occurredAt());

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
