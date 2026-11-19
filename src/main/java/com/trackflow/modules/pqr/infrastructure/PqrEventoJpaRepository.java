package com.trackflow.modules.pqr.infrastructure;

import com.trackflow.modules.pqr.domain.PqrEvento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PqrEventoJpaRepository extends JpaRepository<PqrEvento, Long> {

    List<PqrEvento> findByRadicadoOrderByIdAsc(String radicado);
}
