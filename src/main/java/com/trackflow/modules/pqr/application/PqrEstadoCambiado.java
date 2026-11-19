package com.trackflow.modules.pqr.application;

import com.trackflow.modules.pqr.domain.EstadoPqr;
import java.time.Instant;

/** Una PQR cambió de estado. No lleva la respuesta: el solicitante la consulta con su radicado. */
public record PqrEstadoCambiado(String radicado, EstadoPqr anterior, EstadoPqr nuevo, String nombre, String email,
        Instant occurredAt) {
}
