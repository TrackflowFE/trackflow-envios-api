package com.trackflow.modules.pqr.application;

import com.trackflow.modules.pqr.domain.EstadoPqr;
import com.trackflow.modules.pqr.domain.Pqr;
import com.trackflow.modules.pqr.domain.PqrEvento;
import com.trackflow.modules.pqr.domain.TipoPqr;
import java.util.List;
import java.util.Optional;

/** Puerto de persistencia de las PQR y de su bitácora. */
public interface PqrRepository {

    Pqr save(Pqr pqr);

    PqrEvento saveEvento(PqrEvento evento);

    Optional<Pqr> findByRadicado(String radicado);

    /** Bloqueada, para que dos cambios de estado simultáneos se apliquen uno tras otro. */
    Optional<Pqr> findByRadicadoForUpdate(String radicado);

    /** Más recientes primero; el estado y el tipo son filtros opcionales. */
    List<Pqr> search(EstadoPqr estado, TipoPqr tipo, int pagina, int tamano);

    /** Los cambios de la PQR, del más antiguo al más reciente. */
    List<PqrEvento> historial(String radicado);
}
