package com.trackflow.modules.pickups.domain;

import java.time.LocalDate;

/**
 * La fecha pedida ya pasó: no se puede recoger en el pasado.
 */
public class FechaDeRecogidaEnElPasadoException extends RuntimeException {

    public FechaDeRecogidaEnElPasadoException(LocalDate fecha, LocalDate hoy) {
        super("La fecha de la recogida " + fecha + " ya pasó; hoy es " + hoy);
    }
}
