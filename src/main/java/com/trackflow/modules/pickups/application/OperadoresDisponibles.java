package com.trackflow.modules.pickups.application;

import com.trackflow.modules.pickups.domain.OperadorDesconocidoException;
import java.util.List;

/**
 * Operadores a los que se puede asignar una recogida.
 *
 * Una lectura de referencia de los usuarios del sistema: el módulo de recogidas no
 * administra usuarios, solo necesita saber a quién puede encargarle una visita.
 */
public interface OperadoresDisponibles {

    List<String> activos();

    /**
     * Falla si el usuario no es un operador activo.
     */
    default void exigir(String username) {
        if (username == null || username.isBlank()) {
            throw new OperadorDesconocidoException(username);
        }

        if (!activos().contains(username)) {
            throw new OperadorDesconocidoException(username);
        }
    }
}
