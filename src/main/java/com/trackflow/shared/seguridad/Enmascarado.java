package com.trackflow.shared.seguridad;

/**
 * Enmascara datos personales para mostrarlos sin exponerlos (HU-13, Ley 1581 de 2012).
 *
 * Deja a la vista lo justo para que una persona reconozca el dato que ya conoce
 * —confirmar que el teléfono termina en 45— sin que sirva para contactar a nadie
 * ni para suplantarlo.
 */
public final class Enmascarado {

    private static final int VISIBLES_AL_FINAL = 2;

    private Enmascarado() {
    }

    /**
     * Teléfono: conserva el indicativo y los dos últimos dígitos, como pide el
     * ejemplo de la historia (300 *** **45).
     */
    public static String telefono(String valor) {
        String digitos = soloDigitos(valor);
        if (digitos.length() < 5) {
            return ocultarTodo(digitos);
        }
        return digitos.substring(0, 3) + " *** **" + digitos.substring(digitos.length() - VISIBLES_AL_FINAL);
    }

    /** Documento: solo los dos últimos, porque el resto identifica por sí solo. */
    public static String documento(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.trim();
        if (limpio.length() <= VISIBLES_AL_FINAL) {
            return ocultarTodo(limpio);
        }
        return "*".repeat(limpio.length() - VISIBLES_AL_FINAL)
                + limpio.substring(limpio.length() - VISIBLES_AL_FINAL);
    }

    /**
     * Dirección: deja la parte inicial, que suele ser la vía, y oculta el resto.
     * Sirve para distinguir dos envíos sin publicar dónde vive alguien.
     */
    public static String direccion(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.trim();
        int corte = Math.min(limpio.length(), 8);
        return limpio.substring(0, corte) + (limpio.length() > corte ? " ***" : "");
    }

    private static String ocultarTodo(String valor) {
        return valor == null || valor.isEmpty() ? null : "*".repeat(valor.length());
    }

    private static String soloDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }
}
