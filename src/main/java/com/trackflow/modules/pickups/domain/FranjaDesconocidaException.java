package com.trackflow.modules.pickups.domain;

import java.util.List;

/**
 * La franja pedida no existe en el horario de atención.
 */
public class FranjaDesconocidaException extends RuntimeException {

    public FranjaDesconocidaException(String franja, List<String> franjas) {
        super("La franja " + franja + " no existe en el horario de atención; las disponibles son "
                + String.join(", ", franjas));
    }
}
