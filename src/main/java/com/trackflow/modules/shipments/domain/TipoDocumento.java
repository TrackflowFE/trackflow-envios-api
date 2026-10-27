package com.trackflow.modules.shipments.domain;

import java.util.regex.Pattern;

/**
 * Documentos de identificación admitidos para remitentes y destinatarios.
 *
 * El tipo determina cómo se valida el número: una cédula es numérica, un
 * pasaporte es alfanumérico y un NIT trae dígito de verificación. Por eso son
 * dos datos y no un texto libre.
 */
public enum TipoDocumento {

    CC("Cédula de ciudadanía", false, Pattern.compile("\\d{6,10}")),
    CE("Cédula de extranjería", false, Pattern.compile("\\d{6,10}")),
    TI("Tarjeta de identidad", false, Pattern.compile("\\d{10,11}")),
    PP("Pasaporte", false, Pattern.compile("[A-Z0-9]{5,15}")),
    NIT("Número de identificación tributaria", true, Pattern.compile("\\d{9,10}(-\\d)?"));

    /**
     * Ponderaciones que la DIAN define para el dígito de verificación del NIT,
     * ordenadas desde el último dígito hacia el primero.
     */
    private static final int[] PESOS_NIT = { 3, 7, 13, 17, 19, 23, 29, 37, 41, 43, 47, 53, 59, 67, 71 };

    private final String descripcion;
    private final boolean empresarial;
    private final Pattern formato;

    TipoDocumento(String descripcion, boolean empresarial, Pattern formato) {
        this.descripcion = descripcion;
        this.empresarial = empresarial;
        this.formato = formato;
    }

    public String descripcion() {
        return descripcion;
    }

    /** El NIT identifica empresas; los demás, personas naturales. */
    public boolean esEmpresarial() {
        return empresarial;
    }

    public boolean aceptaNumero(String numero) {
        if (numero == null || !formato.matcher(numero).matches()) {
            return false;
        }
        return this != NIT || digitoDeVerificacionCorrecto(numero);
    }

    public String formatoEsperado() {
        return switch (this) {
            case CC, CE -> "entre 6 y 10 dígitos";
            case TI -> "entre 10 y 11 dígitos";
            case PP -> "entre 5 y 15 caracteres, letras mayúsculas y dígitos";
            case NIT -> "entre 9 y 10 dígitos, opcionalmente con el dígito de verificación (900123456-7)";
        };
    }

    /**
     * Cuando el NIT llega con dígito de verificación, se comprueba: es el control
     * que usan las transportadoras para no facturarle a una empresa inexistente.
     */
    private static boolean digitoDeVerificacionCorrecto(String numero) {
        int separador = numero.indexOf('-');
        if (separador < 0) {
            return true;
        }

        String base = numero.substring(0, separador);
        int declarado = Character.getNumericValue(numero.charAt(separador + 1));

        int suma = 0;
        for (int i = 0; i < base.length(); i++) {
            int digito = Character.getNumericValue(base.charAt(base.length() - 1 - i));
            suma += digito * PESOS_NIT[i];
        }

        int residuo = suma % 11;
        int esperado = residuo > 1 ? 11 - residuo : residuo;

        return declarado == esperado;
    }
}
