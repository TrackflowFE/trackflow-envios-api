package com.trackflow.modules.chatbot.domain;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Deja un texto en una forma comparable: minúsculas, sin tildes y sin signos.
 *
 * "¿Cuánto cuesta?" y "cuanto cuesta" quedan iguales, y el chatbot no depende de que
 * quien escribe acentúe bien.
 */
public final class Normalizador {

    private static final Pattern MARCAS = Pattern.compile("\\p{M}+");
    private static final Pattern NO_ALFANUMERICO = Pattern.compile("[^a-z0-9]+");

    /** Palabras que no dicen de qué se habla; ignorarlas evita coincidencias falsas. */
    private static final Set<String> VACIAS = Set.of("de", "la", "el", "en", "un", "una", "los", "las", "del", "al",
            "mi", "me", "mis", "tu", "tus", "su", "sus", "que", "es", "por", "para", "con", "se", "lo", "le", "muy",
            "quiero", "quisiera", "necesito", "puedo", "favor", "gracias", "hay", "esta", "saber", "algo", "sobre");

    private Normalizador() {
    }

    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = MARCAS.matcher(Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD))
                .replaceAll("");
        return NO_ALFANUMERICO.matcher(sinTildes).replaceAll(" ").strip();
    }

    /** Las palabras con significado de un texto, sin repetir y en orden de aparición. */
    public static Set<String> palabras(String texto) {
        Set<String> palabras = new LinkedHashSet<>();
        for (String palabra : normalizar(texto).split(" ")) {
            if (palabra.length() >= 2 && !VACIAS.contains(palabra)) {
                palabras.add(palabra);
            }
        }
        return palabras;
    }
}
