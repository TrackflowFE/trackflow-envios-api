package com.trackflow.modules.pqr.application;

import java.util.ArrayList;
import java.util.List;

/** Recuerda lo que se publicó, para comprobarlo en las pruebas. */
class PublicadorGrabador implements PublicadorDePqr {

    final List<PqrRadicada> radicadas = new ArrayList<>();
    final List<PqrEstadoCambiado> cambios = new ArrayList<>();

    @Override
    public void radicada(PqrRadicada evento) {
        radicadas.add(evento);
    }

    @Override
    public void estadoCambiado(PqrEstadoCambiado evento) {
        cambios.add(evento);
    }
}
