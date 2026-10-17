package com.trackflow.bootstrap.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SesionesTest {

    private static final Instant INICIO = Instant.parse("2026-10-15T18:00:00Z");
    private static final Instant AHORA = INICIO.plus(Duration.ofMinutes(10));

    private final SesionRepository repositorio = mock(SesionRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final SecurityProperties propiedades = new SecurityProperties("", "operador", "clave", "admin", "", 480,
            30);
    private final Sesiones sesiones = new Sesiones(repositorio, usuarios, propiedades,
            Clock.fixed(AHORA, ZoneOffset.UTC));

    private final Sesion sesion = new Sesion("jti-1", "operador", INICIO, INICIO.plus(Duration.ofHours(8)));

    private void usuario(boolean activo) {
        when(usuarios.findById("operador"))
                .thenReturn(Optional.of(new Usuario("operador", "hash", "OPERADOR", activo, INICIO)));
    }

    @Test
    void abrirBorraLasVencidasYGuardaLaSesionNueva() {
        Instant expira = INICIO.plus(Duration.ofHours(8));

        sesiones.abrir("jti-1", "operador", INICIO, expira);

        verify(repositorio).borrarVencidas(INICIO);
        ArgumentCaptor<Sesion> guardada = ArgumentCaptor.forClass(Sesion.class);
        verify(repositorio).save(guardada.capture());
        assertTrue(guardada.getValue().abierta());
        assertEquals("operador", guardada.getValue().getUsername());
    }

    @Test
    void cerrarCierraLaSesionDelToken() {
        when(repositorio.findById("jti-1")).thenReturn(Optional.of(sesion));

        sesiones.cerrar("jti-1");

        assertFalse(sesion.abierta());
    }

    @Test
    void cerrarUnaSesionInexistenteNoFalla() {
        when(repositorio.findById("otro")).thenReturn(Optional.empty());

        sesiones.cerrar("otro");

        verify(repositorio).findById("otro");
    }

    @Test
    void unaSesionVigenteDeUnUsuarioActivoEsValidaYRegistraActividad() {
        when(repositorio.findById("jti-1")).thenReturn(Optional.of(sesion));
        usuario(true);

        assertTrue(sesiones.validarYRenovar("jti-1"));

        assertTrue(sesion.abierta());
        // La actividad se movió a AHORA: a los 30 minutos desde el inicio todavía no está inactiva.
        assertFalse(sesion.inactivaDesde(INICIO.plus(Duration.ofMinutes(35)), Duration.ofMinutes(30)));
    }

    @Test
    void unTokenSinIdentificadorNoEsValido() {
        assertFalse(sesiones.validarYRenovar(null));
        verifyNoInteractions(repositorio, usuarios);
    }

    @Test
    void unaSesionQueNoExisteNoEsValida() {
        when(repositorio.findById("jti-x")).thenReturn(Optional.empty());

        assertFalse(sesiones.validarYRenovar("jti-x"));
    }

    @Test
    void unaSesionCerradaNoEsValida() {
        sesion.cerrar(INICIO.plusSeconds(5));
        when(repositorio.findById("jti-1")).thenReturn(Optional.of(sesion));

        assertFalse(sesiones.validarYRenovar("jti-1"));
        verifyNoInteractions(usuarios);
    }

    @Test
    void laSesionDeUnUsuarioDesactivadoSeCierraEnElActo() {
        when(repositorio.findById("jti-1")).thenReturn(Optional.of(sesion));
        usuario(false);

        assertFalse(sesiones.validarYRenovar("jti-1"));
        assertFalse(sesion.abierta());
    }

    @Test
    void laSesionDeUnUsuarioQueYaNoExisteSeCierra() {
        when(repositorio.findById("jti-1")).thenReturn(Optional.of(sesion));
        when(usuarios.findById(any())).thenReturn(Optional.empty());

        assertFalse(sesiones.validarYRenovar("jti-1"));
        assertFalse(sesion.abierta());
    }

    @Test
    void unaSesionInactivaMasDelLimiteSeCierra() {
        Sesion vieja = new Sesion("jti-2", "operador", AHORA.minus(Duration.ofMinutes(31)),
                AHORA.plus(Duration.ofHours(1)));
        when(repositorio.findById("jti-2")).thenReturn(Optional.of(vieja));
        usuario(true);

        assertFalse(sesiones.validarYRenovar("jti-2"));
        assertFalse(vieja.abierta());
    }
}
