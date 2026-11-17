package com.trackflow.modules.pickups.domain;

import java.time.LocalDate;

/**
 * La fecha queda más lejos de lo que se permite agendar con antelación.
 */
public class FechaFueraDelHorizonteException extends RuntimeException {

    public FechaFueraDelHorizonteException(LocalDate fecha, int horizonteDias) {
        super("La fecha " + fecha + " está fuera del horizonte de programación: se recogen hasta "
                + horizonteDias + " días por adelantado");
    }
}
