package com.trackflow.modules.pickups.infrastructure;

import com.trackflow.modules.pickups.application.PickupEventRepository;
import com.trackflow.modules.pickups.domain.PickupEvent;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPickupEventRepository implements PickupEventRepository {

    private final PickupEventJpaRepository jpa;

    public JpaPickupEventRepository(PickupEventJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public PickupEvent save(PickupEvent event) {
        return jpa.save(event);
    }

    @Override
    public boolean existsByEventId(String eventId) {
        return jpa.existsByEventId(eventId);
    }

    @Override
    public List<PickupEvent> findByPickupNumber(String pickupNumber) {
        return jpa.findByPickupNumberOrderByOccurredAtAscIdAsc(pickupNumber);
    }
}
