package com.trackflow.modules.pqr.application;

/**
 * Avisa de lo que pasa con una PQR a quien quiera enterarse (hoy, el correo al
 * solicitante). El caso de uso no sabe quién escucha ni cómo.
 */
public interface PublicadorDePqr {

    void radicada(PqrRadicada evento);

    void estadoCambiado(PqrEstadoCambiado evento);
}
