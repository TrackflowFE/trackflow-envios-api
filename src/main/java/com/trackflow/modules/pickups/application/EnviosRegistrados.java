package com.trackflow.modules.pickups.application;

import com.trackflow.modules.pickups.domain.EnvioVinculadoFueraDeCiudadException;
import com.trackflow.modules.pickups.domain.EnvioVinculadoNoEncontradoException;
import com.trackflow.modules.pickups.domain.EnvioYaEnRecorridoException;
import java.util.Optional;

/**
 * Envíos que se pueden vincular a una recogida.
 *
 * Es una lectura de referencia declarada, como las de cobertura y de red de rutas:
 * el módulo de recogidas solo necesita confirmar que la guía existe y de qué ciudad
 * parte, no el modelo completo del envío.
 */
public interface EnviosRegistrados {

    /** Un envío del que solo hace falta el número y su ciudad de origen. */
    record EnvioRegistrado(String trackingNumber, Long originCityId, String status) {
    }

    Optional<EnvioRegistrado> buscar(String trackingNumber);

    /**
     * Exige que el envío exista y que salga de la misma ciudad donde se va a
     * recoger: recoger en una ciudad un paquete que viaja desde otra no tiene
     * sentido.
     */
    default void exigir(String trackingNumber, Long ciudadRecogida) {
        EnvioRegistrado envio = buscar(trackingNumber)
                .orElseThrow(() -> new EnvioVinculadoNoEncontradoException(trackingNumber));

        if (!envio.originCityId().equals(ciudadRecogida)) {
            throw new EnvioVinculadoFueraDeCiudadException(trackingNumber, ciudadRecogida);
        }
        if (!"REGISTERED".equals(envio.status())) {
            throw new EnvioYaEnRecorridoException(trackingNumber);
        }
    }
}
