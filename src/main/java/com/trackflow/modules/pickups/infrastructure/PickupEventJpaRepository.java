package com.trackflow.modules.pickups.infrastructure;

import com.trackflow.modules.pickups.application.PickupEventRepository;
import com.trackflow.modules.pickups.domain.PickupEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PickupEventJpaRepository extends JpaRepository<PickupEvent, Long> {

    boolean existsByEventId(String eventId);

    List<PickupEvent> findByPickupNumberOrderByOccurredAtAscIdAsc(String pickupNumber);
}
