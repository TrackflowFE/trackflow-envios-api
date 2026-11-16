package com.trackflow.modules.pickups.infrastructure;

import com.trackflow.modules.pickups.application.PickupRepository;
import com.trackflow.modules.pickups.domain.Pickup;
import com.trackflow.modules.pickups.domain.PickupStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PickupJpaRepository extends JpaRepository<Pickup, Long> {

    Optional<Pickup> findByPickupNumberValue(String pickupNumber);

    boolean existsByPickupNumberValue(String pickupNumber);

    @Query("select count(p) > 0 from Pickup p where p.shipmentTrackingNumber = :trackingNumber "
            + "and p.status <> com.trackflow.modules.pickups.domain.PickupStatus.CANCELLED "
            + "and (:pickupNumber is null or p.pickupNumber.value <> :pickupNumber)")
    boolean existsActiveByShipmentExcept(@Param("trackingNumber") String trackingNumber,
            @Param("pickupNumber") String pickupNumber);

    List<Pickup> findByStatus(PickupStatus status);

    List<Pickup> findAllByOrderByIdAsc();

    /**
     * La recogida bloqueada: dos comandos sobre la misma recogida se aplican uno
     * detrás del otro, y el segundo se valida contra un estado que ya incluye al
     * primero.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pickup p where p.pickupNumber.value = :pickupNumber")
    Optional<Pickup> findByNumberParaActualizar(@Param("pickupNumber") String pickupNumber);
}
