package com.trackflow.modules.pickups.domain;

import java.time.Duration;

/**
 * La franja empieza demasiado pronto para poder atenderla con la antelación mínima.
 */
public class SinAnticipacionSuficienteException extends RuntimeException {

    public SinAnticipacionSuficienteException(String franja, Duration anticipacionMinima) {
        super("La franja " + franja + " no da tiempo para recoger: hace falta avisar con al menos "
                + anticipacionMinima.toHours() + " horas de antelación");
    }
}
