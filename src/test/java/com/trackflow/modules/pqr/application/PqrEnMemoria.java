package com.trackflow.modules.pqr.application;

import com.trackflow.modules.pqr.domain.EstadoPqr;
import com.trackflow.modules.pqr.domain.Pqr;
import com.trackflow.modules.pqr.domain.PqrEvento;
import com.trackflow.modules.pqr.domain.TipoPqr;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Repositorio en memoria para probar los casos de uso sin base de datos. */
class PqrEnMemoria implements PqrRepository {

    final List<Pqr> pqrs = new ArrayList<>();
    final List<PqrEvento> eventos = new ArrayList<>();

    @Override
    public Pqr save(Pqr pqr) {
        if (!pqrs.contains(pqr)) {
            pqrs.add(pqr);
        }
        return pqr;
    }

    @Override
    public PqrEvento saveEvento(PqrEvento evento) {
        eventos.add(evento);
        return evento;
    }

    @Override
    public Optional<Pqr> findByRadicado(String radicado) {
        return pqrs.stream().filter(p -> p.radicado().equals(radicado)).findFirst();
    }

    @Override
    public Optional<Pqr> findByRadicadoForUpdate(String radicado) {
        return findByRadicado(radicado);
    }

    @Override
    public List<Pqr> search(EstadoPqr estado, TipoPqr tipo, int pagina, int tamano) {
        return pqrs.stream()
                .filter(p -> estado == null || p.estado() == estado)
                .filter(p -> tipo == null || p.tipo() == tipo)
                .skip((long) pagina * tamano).limit(tamano).toList();
    }

    @Override
    public List<PqrEvento> historial(String radicado) {
        return eventos.stream().filter(e -> e.radicado().equals(radicado)).toList();
    }
}
