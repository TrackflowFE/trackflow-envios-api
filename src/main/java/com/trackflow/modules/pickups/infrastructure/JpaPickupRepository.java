package com.trackflow.modules.pickups.infrastructure;

import com.trackflow.modules.pickups.application.PickupRepository;
import com.trackflow.modules.pickups.domain.Pickup;
import com.trackflow.modules.pickups.domain.PickupStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPickupRepository implements PickupRepository {

    private final PickupJpaRepository jpa;

    public JpaPickupRepository(PickupJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Pickup save(Pickup pickup) {
        return jpa.save(pickup);
    }

    @Override
    public Optional<Pickup> findByNumber(String pickupNumber) {
        return jpa.findByPickupNumberValue(pickupNumber);
    }

    @Override
    public Optional<Pickup> findByNumberForUpdate(String pickupNumber) {
        return jpa.findByNumberParaActualizar(pickupNumber);
    }

    @Override
    public boolean existsByNumber(String pickupNumber) {
        return jpa.existsByPickupNumberValue(pickupNumber);
    }

    @Override
    public List<Pickup> search(PickupStatus status) {
        return status == null ? jpa.findAllByOrderByIdAsc() : jpa.findByStatus(status);
    }
}
