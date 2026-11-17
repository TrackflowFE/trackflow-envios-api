package com.trackflow.modules.pickups.infrastructure;

import com.trackflow.modules.pickups.application.PickupNumberGenerator;
import com.trackflow.modules.pickups.application.PickupRepository;
import com.trackflow.modules.pickups.domain.PickupNumber;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UuidPickupNumberGenerator implements PickupNumberGenerator {

    private static final int SUFFIX_LENGTH = 12;

    private final PickupRepository recogidas;

    public UuidPickupNumberGenerator(PickupRepository recogidas) {
        this.recogidas = recogidas;
    }

    @Override
    public PickupNumber next() {
        PickupNumber candidate;
        do {
            candidate = PickupNumber.of(PickupNumber.PREFIX + UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, SUFFIX_LENGTH)
                    .toUpperCase());
        } while (recogidas.existsByNumber(candidate.value()));

        return candidate;
    }
}
