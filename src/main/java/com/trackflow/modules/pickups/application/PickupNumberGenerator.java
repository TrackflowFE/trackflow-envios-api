package com.trackflow.modules.pickups.application;

import com.trackflow.modules.pickups.domain.PickupNumber;

/**
 * Puerto de generación del número de recogida.
 *
 * El número se genera en la admisión, no al aplicar el comando, para poder
 * entregárselo a quien pide la recogida en el momento, igual que la guía de un envío.
 */
public interface PickupNumberGenerator {

    PickupNumber next();
}
