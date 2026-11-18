package com.trackflow.bootstrap.security;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cuándo se bloquea una cuenta por intentos fallidos y por cuánto tiempo (HU-09).
 *
 * Son tres valores que solo tienen sentido juntos, y por eso viven aparte de
 * SecurityProperties: quien lea esta clase ve la regla completa de un vistazo.
 */
@Component
public class BloqueoProperties {

    private final int intentos;
    private final Duration ventana;
    private final Duration duracion;

    public BloqueoProperties(
            @Value("${trackflow.security.intentos-antes-de-bloquear:5}") int intentos,
            @Value("${trackflow.security.ventana-intentos-minutos:15}") long ventanaMinutos,
            @Value("${trackflow.security.bloqueo-minutos:15}") long bloqueoMinutos) {
        this.intentos = intentos;
        this.ventana = Duration.ofMinutes(ventanaMinutos);
        this.duracion = Duration.ofMinutes(bloqueoMinutos);
    }

    /** Fallos que bloquean la cuenta, si ocurren dentro de la ventana. */
    public int intentos() {
        return intentos;
    }

    /**
     * Plazo dentro del cual los fallos se consideran seguidos. Un fallo aislado hace
     * meses no debe acercar la cuenta al bloqueo: quien se equivoca al teclear una
     * vez cada tanto no es quien prueba contraseñas en serie.
     */
    public Duration ventana() {
        return ventana;
    }

    /** Cuánto dura el bloqueo si nadie lo levanta antes. */
    public Duration duracion() {
        return duracion;
    }
}
