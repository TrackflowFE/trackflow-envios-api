package com.trackflow.modules.pqr.application;

import com.trackflow.modules.pqr.domain.TipoPqr;
import java.time.Instant;

/** Se radicó una PQR: lo necesario para escribirle al solicitante, nada de su caso. */
public record PqrRadicada(String radicado, TipoPqr tipo, String nombre, String email, Instant occurredAt) {
}
