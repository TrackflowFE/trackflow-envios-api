package com.trackflow.modules.pickups.application;

/**
 * Puerto de publicación de los comandos de recogida.
 *
 * La publicación va al broker y el cambio lo aplica el consumidor: ninguna petición
 * HTTP escribe en la base, igual que en el resto de la API.
 */
public interface RecogidaComandoPublisher {

    void publicar(RecogidaComando comando);
}
