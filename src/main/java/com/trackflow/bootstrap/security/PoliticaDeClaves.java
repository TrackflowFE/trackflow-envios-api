package com.trackflow.bootstrap.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Las reglas que debe cumplir una contraseña (HU-10). Viven en un solo sitio para
 * que el alta de usuarios, el cambio de clave y la recuperación exijan lo mismo.
 *
 * Cuando una clave no pasa, el mensaje dice qué regla incumple: pedir "una clave
 * válida" sin explicar qué falta solo lleva a que la persona pruebe a ciegas.
 */
@Component
public class PoliticaDeClaves {

    /** Longitud mínima exigida por la historia. */
    public static final int MINIMO = 10;

    static final String MUY_CORTA = "La contraseña debe tener al menos " + MINIMO + " caracteres";
    static final String IGUAL_A_LA_ANTERIOR = "La nueva contraseña debe ser distinta de la anterior";

    private final PasswordEncoder encoder;

    public PoliticaDeClaves(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    /** Para un alta, donde no hay clave anterior con la que comparar. */
    public void exigir(String clave) {
        exigir(clave, null);
    }

    /**
     * @param hashAnterior hash de la clave vigente, o null si no hay ninguna
     */
    public void exigir(String clave, String hashAnterior) {
        if (clave == null || clave.length() < MINIMO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MUY_CORTA);
        }
        if (hashAnterior != null && encoder.matches(clave, hashAnterior)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, IGUAL_A_LA_ANTERIOR);
        }
    }
}
