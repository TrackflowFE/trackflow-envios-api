package com.trackflow.bootstrap.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UsuarioTest {

    @Test
    void guardaLosDatosYPermiteCambiarElHash() {
        Usuario usuario = new Usuario("admin", "hash-1", "ADMIN OPERADOR", true,
                Instant.parse("2026-10-15T18:00:00Z"));

        usuario.cambiarPasswordHash("hash-2");

        assertEquals("admin", usuario.getUsername());
        assertEquals("hash-2", usuario.getPasswordHash());
        assertEquals("ADMIN OPERADOR", usuario.getRoles());
        assertTrue(usuario.isActive());
    }
}
