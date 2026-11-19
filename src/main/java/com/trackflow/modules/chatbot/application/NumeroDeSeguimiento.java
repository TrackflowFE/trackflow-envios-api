package com.trackflow.modules.chatbot.application;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Detecta si un mensaje trae algo con forma de número de guía (TF + 12 letras o números).
 *
 * Solo mira la forma: no comprueba que el envío exista. Así la respuesta es idéntica
 * para una guía real y para una inventada, y el chatbot no sirve para averiguar cuáles
 * existen. Admite espacios, puntos o guiones dentro del número, pero exige que "TF"
 * no sea la cola de otra palabra.
 */
final class NumeroDeSeguimiento {

    private static final Pattern GUIA = Pattern.compile("(?<![A-Z0-9])T\\s*F(?:[\\s.\\-_]*[0-9A-Z]){12}(?![A-Z0-9])");

    private NumeroDeSeguimiento() {
    }

    static boolean contiene(String mensaje) {
        return GUIA.matcher(mensaje.toUpperCase(Locale.ROOT)).find();
    }
}
