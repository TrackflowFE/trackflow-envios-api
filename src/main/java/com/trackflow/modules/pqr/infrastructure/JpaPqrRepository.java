package com.trackflow.modules.pqr.infrastructure;

import com.trackflow.modules.pqr.application.PqrRepository;
import com.trackflow.modules.pqr.domain.EstadoPqr;
import com.trackflow.modules.pqr.domain.Pqr;
import com.trackflow.modules.pqr.domain.PqrEvento;
import com.trackflow.modules.pqr.domain.TipoPqr;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPqrRepository implements PqrRepository {

    private final PqrJpaRepository jpa;
    private final PqrEventoJpaRepository eventos;

    public JpaPqrRepository(PqrJpaRepository jpa, PqrEventoJpaRepository eventos) {
        this.jpa = jpa;
        this.eventos = eventos;
    }

    @Override
    public Pqr save(Pqr pqr) {
        return jpa.save(pqr);
    }

    @Override
    public PqrEvento saveEvento(PqrEvento evento) {
        return eventos.save(evento);
    }

    @Override
    public Optional<Pqr> findByRadicado(String radicado) {
        return jpa.findByRadicado(radicado);
    }

    @Override
    public Optional<Pqr> findByRadicadoForUpdate(String radicado) {
        return jpa.findByRadicadoParaActualizar(radicado);
    }

    @Override
    public List<Pqr> search(EstadoPqr estado, TipoPqr tipo, int pagina, int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano);
        if (estado != null && tipo != null) {
            return jpa.findByEstadoAndTipoOrderByIdDesc(estado, tipo, pageable);
        }
        if (estado != null) {
            return jpa.findByEstadoOrderByIdDesc(estado, pageable);
        }
        if (tipo != null) {
            return jpa.findByTipoOrderByIdDesc(tipo, pageable);
        }
        return jpa.findAllByOrderByIdDesc(pageable);
    }

    @Override
    public List<PqrEvento> historial(String radicado) {
        return eventos.findByRadicadoOrderByIdAsc(radicado);
    }
}
