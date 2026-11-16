package com.trackflow.modules.pickups.application;

import com.trackflow.modules.pickups.domain.Pickup;
import com.trackflow.modules.pickups.domain.PickupStatus;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia de las recogidas.
 *
 * Es el módulo de recogidas el que guarda sus propias tablas: API 1 es la dueña del
 * esquema, y así las recogidas no dependen de que exista un envío.
 */
public interface PickupRepository {

    Pickup save(Pickup pickup);

    Optional<Pickup> findByNumber(String pickupNumber);

    /**
     * La recogida bloqueada para poder modificarla.
     *
     * El bloqueo importa: dos comandos sobre la misma recogida se aplican uno detrás
     * del otro, y el segundo se valida contra un estado que ya incluye al primero.
     * Sin él, dos cancelaciones simultáneas pasarían la misma validación.
     */
    Optional<Pickup> findByNumberForUpdate(String pickupNumber);

    boolean existsByNumber(String pickupNumber);

    /** Todas las recogidas, o solo las de un estado, para el listado de la operación. */
    List<Pickup> search(PickupStatus status);
}
