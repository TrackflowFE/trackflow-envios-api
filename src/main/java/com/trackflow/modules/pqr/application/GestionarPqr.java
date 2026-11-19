package com.trackflow.modules.pqr.application;

import com.trackflow.modules.pqr.domain.EstadoPqr;
import com.trackflow.modules.pqr.domain.Pqr;
import com.trackflow.modules.pqr.domain.PqrEvento;
import com.trackflow.modules.pqr.domain.PqrNoEncontradaException;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HU-19: quien atiende responde la PQR y la mueve de estado. */
@Service
public class GestionarPqr {

    private final PqrRepository pqrs;
    private final PublicadorDePqr publicador;
    private final Clock clock;

    public GestionarPqr(PqrRepository pqrs, PublicadorDePqr publicador, Clock clock) {
        this.pqrs = pqrs;
        this.publicador = publicador;
        this.clock = clock;
    }

    @Transactional
    public VistaDePqr cambiarEstado(String radicado, EstadoPqr destino, String respuesta, String actor) {
        Pqr pqr = pqrs.findByRadicadoForUpdate(radicado).orElseThrow(PqrNoEncontradaException::new);
        EstadoPqr anterior = pqr.estado();
        PqrEvento evento = pqr.cambiarEstado(destino, TextoPlano.limpiar(respuesta), actor, clock.instant());
        pqrs.save(pqr);
        pqrs.saveEvento(evento);
        publicador.estadoCambiado(new PqrEstadoCambiado(radicado, anterior, destino, pqr.contactoNombre(),
                pqr.contactoEmail(), evento.occurredAt()));
        return new VistaDePqr(pqr, pqrs.historial(radicado));
    }
}
