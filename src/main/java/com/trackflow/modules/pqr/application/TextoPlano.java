package com.trackflow.modules.pqr.application;

/**
 * Deja un texto de usuario listo para guardar.
 *
 * No escapa HTML: el texto se guarda como llegó. Solo quita lo que no es texto (los
 * caracteres de control, salvo el salto de línea y el tabulador) y los espacios de los
 * extremos.
 */
final class TextoPlano {

    private TextoPlano() {
    }

    static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        StringBuilder limpio = new StringBuilder(texto.length());
        texto.strip().codePoints()
                .filter(c -> !Character.isISOControl(c) || c == '\n' || c == '\t')
                .forEach(limpio::appendCodePoint);
        return limpio.toString();
    }
}
