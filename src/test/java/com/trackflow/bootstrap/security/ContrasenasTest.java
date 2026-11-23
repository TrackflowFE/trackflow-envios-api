package com.trackflow.bootstrap.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class ContrasenasTest {

    private static final Instant AHORA = Instant.parse("2026-11-01T15:00:00Z");

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final EnlacesDeRecuperacion enlaces = mock(EnlacesDeRecuperacion.class);
    private final CorreoDeRecuperacion correo = mock(CorreoDeRecuperacion.class);
    private final Sesiones sesiones = mock(Sesiones.class);
    private final Auditorias auditorias = mock(Auditorias.class);
    private final PoliticaDeClaves politica = mock(PoliticaDeClaves.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final Contrasenas contrasenas =
            new Contrasenas(usuarios, enlaces, correo, sesiones, auditorias, politica, encoder);

    private Usuario ana(boolean activa) {
        Usuario usuario = new Usuario("ana", "hash-vigente", "OPERADOR", activa, AHORA);
        usuario.cambiarEmail("ana@ejemplo.com");
        return usuario;
    }

    // --- cambiar ---

    @Test
    void cambiarConUnUsuarioInexistenteDaNoAutorizado() {
        when(usuarios.findById("ana")).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> contrasenas.cambiar("ana", "jti-1", "actual", "nueva-clave-123"));

        assertEquals(401, error.getStatusCode().value());
        assertEquals(Contrasenas.CLAVE_ACTUAL_NO_COINCIDE, error.getReason());
    }

    @Test
    void cambiarConLaClaveActualIncorrectaNoConsultaLaPoliticaNiCierraSesiones() {
        when(usuarios.findById("ana")).thenReturn(Optional.of(ana(true)));
        when(encoder.matches("equivocada", "hash-vigente")).thenReturn(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> contrasenas.cambiar("ana", "jti-1", "equivocada", "nueva-clave-123"));

        assertEquals(401, error.getStatusCode().value());
        verifyNoInteractions(politica, sesiones, auditorias);
    }

    @Test
    void cambiarGuardaLaClaveNuevaCierraLasOtrasSesionesYAudita() {
        Usuario usuario = ana(true);
        when(usuarios.findById("ana")).thenReturn(Optional.of(usuario));
        when(encoder.matches("actual-correcta", "hash-vigente")).thenReturn(true);
        when(encoder.encode("nueva-clave-123")).thenReturn("hash-nuevo");
        when(sesiones.cerrarTodasDe("ana", "jti-1")).thenReturn(2);

        contrasenas.cambiar("ana", "jti-1", "actual-correcta", "nueva-clave-123");

        verify(politica).exigir("nueva-clave-123", "hash-vigente");
        assertEquals("hash-nuevo", usuario.getPasswordHash());
        assertTrue(usuario.isPasswordSetByUser());
        verify(auditorias).registrar("ana", Auditorias.CLAVE_CAMBIADA, "ana",
                "por su titular; otras sesiones cerradas: 2");
    }

    @Test
    void cambiarConUnaClaveQueIncumpleLaPoliticaNoCierraSesiones() {
        Usuario usuario = ana(true);
        when(usuarios.findById("ana")).thenReturn(Optional.of(usuario));
        when(encoder.matches("actual-correcta", "hash-vigente")).thenReturn(true);
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, PoliticaDeClaves.MUY_CORTA))
                .when(politica).exigir("corta", "hash-vigente");

        assertThrows(ResponseStatusException.class,
                () -> contrasenas.cambiar("ana", "jti-1", "actual-correcta", "corta"));

        assertEquals("hash-vigente", usuario.getPasswordHash());
        verifyNoInteractions(sesiones, auditorias);
    }

    // --- solicitar ---

    @Test
    void solicitarSinCuentaNoEnviaNada() {
        when(usuarios.findByEmailIgnoreCase("nadie@ejemplo.com")).thenReturn(Optional.empty());

        contrasenas.solicitar("nadie@ejemplo.com");

        verifyNoInteractions(enlaces, correo);
    }

    @Test
    void solicitarConLaCuentaDesactivadaNoEnviaNada() {
        when(usuarios.findByEmailIgnoreCase("ana@ejemplo.com")).thenReturn(Optional.of(ana(false)));

        contrasenas.solicitar("ana@ejemplo.com");

        verifyNoInteractions(enlaces, correo);
    }

    @Test
    void solicitarConCuentaActivaEmiteElEnlaceYEnviaElCorreo() {
        when(usuarios.findByEmailIgnoreCase("ana@ejemplo.com")).thenReturn(Optional.of(ana(true)));
        when(enlaces.emitirPara("ana")).thenReturn("token-123");

        contrasenas.solicitar("  ana@ejemplo.com  ");

        verify(correo).enviar("ana@ejemplo.com", "ana", "token-123");
    }

    // --- restablecer ---

    @Test
    void restablecerConUnEnlaceInvalidoPropagaElErrorSinTocarNada() {
        when(enlaces.consumir("malo")).thenThrow(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, EnlacesDeRecuperacion.ENLACE_INVALIDO));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> contrasenas.restablecer("malo", "nueva-clave-123"));

        assertEquals(EnlacesDeRecuperacion.ENLACE_INVALIDO, error.getReason());
        verifyNoInteractions(politica, sesiones, auditorias);
    }

    @Test
    void restablecerSiElUsuarioYaNoExisteResponde400() {
        when(enlaces.consumir("token-123")).thenReturn("ana");
        when(usuarios.findById("ana")).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> contrasenas.restablecer("token-123", "nueva-clave-123"));

        assertEquals(400, error.getStatusCode().value());
        assertEquals(EnlacesDeRecuperacion.ENLACE_INVALIDO, error.getReason());
    }

    @Test
    void restablecerGuardaLaClaveCierraTodasLasSesionesYAudita() {
        Usuario usuario = ana(true);
        when(enlaces.consumir("token-123")).thenReturn("ana");
        when(usuarios.findById("ana")).thenReturn(Optional.of(usuario));
        when(encoder.encode("nueva-clave-123")).thenReturn("hash-nuevo");
        when(sesiones.cerrarTodasDe("ana", null)).thenReturn(3);

        contrasenas.restablecer("token-123", "nueva-clave-123");

        verify(politica).exigir("nueva-clave-123", "hash-vigente");
        assertEquals("hash-nuevo", usuario.getPasswordHash());
        assertTrue(usuario.isPasswordSetByUser());
        verify(auditorias).registrar("ana", Auditorias.CLAVE_CAMBIADA, "ana",
                "por recuperación con enlace; sesiones cerradas: 3");
    }

    @Test
    void restablecerConUnaClaveQueIncumpleLaPoliticaNoCambiaNada() {
        Usuario usuario = ana(true);
        when(enlaces.consumir("token-123")).thenReturn("ana");
        when(usuarios.findById("ana")).thenReturn(Optional.of(usuario));
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, PoliticaDeClaves.MUY_CORTA))
                .when(politica).exigir("corta", "hash-vigente");

        assertThrows(ResponseStatusException.class, () -> contrasenas.restablecer("token-123", "corta"));

        assertEquals("hash-vigente", usuario.getPasswordHash());
        assertFalse(usuario.isPasswordSetByUser());
        verify(sesiones, never()).cerrarTodasDe(anyString(), any());
    }
}
