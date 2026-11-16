package com.trackflow.modules.pickups.application;

import com.trackflow.modules.pickups.domain.PickupEvent;
import java.util.List;

/**
 * Puerto de persistencia de la bitácora de las recogidas.
 */
public interface PickupEventRepository {

    PickupEvent save(PickupEvent event);

    /**
     * El broker entrega al menos una vez, así que un mismo cambio puede llegar dos
     * veces; la segunda se descarta en lugar de duplicar la bitácora.
     */
    boolean existsByEventId(String eventId);

    List<PickupEvent> findByPickupNumber(String pickupNumber);
}
