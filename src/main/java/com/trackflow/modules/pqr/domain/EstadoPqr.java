package com.trackflow.modules.pqr.domain;

import java.util.Set;

/**
 * Las etapas de una PQR.
 *
 * Avanza en orden: recibida, en trámite, resuelta, cerrada. El único paso atrás es
 * reabrir una resuelta, para el caso de que la respuesta no haya bastado. Cerrada es
 * final.
 */
public enum EstadoPqr {
    RECIBIDA,
    EN_TRAMITE,
    RESUELTA,
    CERRADA;

    public boolean puedePasarA(EstadoPqr destino) {
        return siguientes().contains(destino);
    }

    /** Solo en estos estados el solicitante ve la respuesta. */
    public boolean muestraRespuesta() {
        return this == RESUELTA || this == CERRADA;
    }

    private Set<EstadoPqr> siguientes() {
        return switch (this) {
            case RECIBIDA -> Set.of(EN_TRAMITE);
            case EN_TRAMITE -> Set.of(RESUELTA);
            case RESUELTA -> Set.of(CERRADA, EN_TRAMITE);
            case CERRADA -> Set.of();
        };
    }
}
