package com.trackflow.bootstrap.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class UsuarioInactivoDePruebaTest {

    private static final Instant AHORA = Instant.parse("2026-10-15T18:00:00Z");

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);

    private Usuario creadoCon(String clave) {
        when(usuarios.existsById("inactivo")).thenReturn(false);

        new UsuarioInactivoDePrueba(usuarios, encoder, clock, clave).run(null);

        ArgumentCaptor<Usuario> creado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(creado.capture());
        return creado.getValue();
    }

    @Test
    void creaElUsuarioDesactivadoConLaClaveConfigurada() {
        Usuario usuario = creadoCon("clave-de-prueba");

        assertEquals(UsuarioInactivoDePrueba.USUARIO, usuario.getUsername());
        assertEquals(SecurityProperties.ROL_OPERADOR, usuario.getRoles());
        assertFalse(usuario.isActive());
        assertTrue(encoder.matches("clave-de-prueba", usuario.getPasswordHash()));
    }

    @Test
    void sinClaveConfiguradaUsaUnaAleatoriaQueNadieConoce() {
        Usuario usuario = creadoCon("  ");

        assertFalse(usuario.isActive());
        assertFalse(encoder.matches("  ", usuario.getPasswordHash()));
        assertFalse(encoder.matches("", usuario.getPasswordHash()));
    }

    @Test
    void siYaExisteNoLoVuelveACrear() {
        when(usuarios.existsById("inactivo")).thenReturn(true);

        new UsuarioInactivoDePrueba(usuarios, encoder, clock, null).run(null);

        verify(usuarios, never()).save(any());
    }
}
