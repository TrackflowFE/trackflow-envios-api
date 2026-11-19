package com.trackflow.modules.pqr.infrastructure;

import com.trackflow.modules.pqr.application.PqrEstadoCambiado;
import com.trackflow.modules.pqr.application.PqrRadicada;
import com.trackflow.modules.pqr.application.PublicadorDePqr;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Publica los eventos dentro del proceso; quien los escucha lo hace tras confirmar la transacción. */
@Component
public class PublicadorDePqrSpring implements PublicadorDePqr {

    private final ApplicationEventPublisher publicador;

    public PublicadorDePqrSpring(ApplicationEventPublisher publicador) {
        this.publicador = publicador;
    }

    @Override
    public void radicada(PqrRadicada evento) {
        publicador.publishEvent(evento);
    }

    @Override
    public void estadoCambiado(PqrEstadoCambiado evento) {
        publicador.publishEvent(evento);
    }
}
