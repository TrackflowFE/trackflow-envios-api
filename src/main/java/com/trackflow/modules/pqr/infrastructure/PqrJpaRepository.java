package com.trackflow.modules.pqr.infrastructure;

import com.trackflow.modules.pqr.domain.EstadoPqr;
import com.trackflow.modules.pqr.domain.Pqr;
import com.trackflow.modules.pqr.domain.TipoPqr;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PqrJpaRepository extends JpaRepository<Pqr, Long> {

    Optional<Pqr> findByRadicado(String radicado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pqr p where p.radicado = :radicado")
    Optional<Pqr> findByRadicadoParaActualizar(@Param("radicado") String radicado);

    List<Pqr> findAllByOrderByIdDesc(Pageable pagina);

    List<Pqr> findByEstadoOrderByIdDesc(EstadoPqr estado, Pageable pagina);

    List<Pqr> findByTipoOrderByIdDesc(TipoPqr tipo, Pageable pagina);

    List<Pqr> findByEstadoAndTipoOrderByIdDesc(EstadoPqr estado, TipoPqr tipo, Pageable pagina);
}
