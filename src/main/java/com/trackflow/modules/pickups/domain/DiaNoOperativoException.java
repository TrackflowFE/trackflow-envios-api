package com.trackflow.modules.pickups.domain;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * No se recoge ese día: la atención no cubre todos los días de la semana.
 */
public class DiaNoOperativoException extends RuntimeException {

    private static final Locale ESPANOL = Locale.forLanguageTag("es");

    public DiaNoOperativoException(LocalDate fecha) {
        super("No se hacen recogidas los " + fecha.getDayOfWeek().getDisplayName(TextStyle.FULL, ESPANOL) + " "
                + fecha + "; elige otro día");
    }
}
