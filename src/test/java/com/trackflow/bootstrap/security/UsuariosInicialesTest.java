package com.trackflow.bootstrap.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class UsuariosInicialesTest {

    private static final Instant AHORA = Instant.parse("2026-10-15T18:00:00Z");

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);

    private UsuariosIniciales con(String claveOperador, String claveAdmin) {
        SecurityProperties propiedades = new SecurityProperties("", "operador", claveOperador, "admin", claveAdmin,
                480, 30);
        return new UsuariosIniciales(usuarios, propiedades, encoder, clock);
    }

    @Test
    void creaOperadorYAdministradorConSusRolesYSoloElHashDeLaClave() {
        when(usuarios.findById(any())).thenReturn(Optional.empty());

        con("clave-op", "clave-admin").run(null);

        ArgumentCaptor<Usuario> creados = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios, times(2)).save(creados.capture());
        Usuario operador = creados.getAllValues().get(0);
        Usuario admin = creados.getAllValues().get(1);
        assertEquals("operador", operador.getUsername());
        assertEquals("OPERADOR", operador.getRoles());
        assertTrue(operador.isActive());
        assertFalse(operador.getPasswordHash().contains("clave-op"));
        assertTrue(encoder.matches("clave-op", operador.getPasswordHash()));
        assertEquals("admin", admin.getUsername());
        assertEquals("ADMIN OPERADOR", admin.getRoles());
        assertTrue(encoder.matches("clave-admin", admin.getPasswordHash()));
    }

    @Test
    void sinClavesNoCreaNingunUsuario() {
        con("", null).run(null);

        verifyNoInteractions(usuarios);
    }

    @Test
    void siLaClaveDelEntornoCambioActualizaElHash() {
        Usuario existente = new Usuario("operador", encoder.encode("vieja"), "OPERADOR", true, AHORA);
        when(usuarios.findById("operador")).thenReturn(Optional.of(existente));

        con("nueva", "").run(null);

        assertTrue(encoder.matches("nueva", existente.getPasswordHash()));
        verify(usuarios, never()).save(any());
    }

    @Test
    void siLaClaveNoCambioNoTocaElHashNiReactivaAlUsuario() {
        String hash = encoder.encode("igual");
        Usuario desactivado = new Usuario("operador", hash, "OPERADOR", false, AHORA);
        when(usuarios.findById("operador")).thenReturn(Optional.of(desactivado));

        con("igual", "").run(null);

        assertEquals(hash, desactivado.getPasswordHash());
        assertFalse(desactivado.isActive());
        verify(usuarios, never()).save(any());
    }
}
