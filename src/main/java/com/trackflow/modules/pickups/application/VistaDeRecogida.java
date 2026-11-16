package com.trackflow.modules.pickups.application;

import com.trackflow.modules.pickups.domain.Pickup;
import com.trackflow.modules.pickups.domain.PickupEvent;
import java.util.List;

/**
 * Lo que la operación necesita saber de una recogida para mostrarla.
 *
 * Reúne la recogida, el nombre de su ciudad y su bitácora, y resuelve si quien
 * consulta tiene derecho a ver los datos privados.
 */
public record VistaDeRecogida(
        Pickup pickup,
        String cityName,
        List<PickupEvent> historial,
        boolean datosPrivadosVisibles) {
}
