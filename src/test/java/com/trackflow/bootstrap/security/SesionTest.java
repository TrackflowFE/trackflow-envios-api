package com.trackflow.bootstrap.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SesionTest {

    private static final Instant INICIO = Instant.parse("2026-10-15T18:00:00Z");
    private static final Duration TREINTA_MINUTOS = Duration.ofMinutes(30);

    private final Sesion sesion = new Sesion("jti-1", "operador", INICIO, INICIO.plus(Duration.ofHours(8)));

    @Test
    void unaSesionNuevaEstaAbiertaYPerteneceAlUsuario() {
        assertTrue(sesion.abierta());
        assertEquals("operador", sesion.getUsername());
    }

    @Test
    void cerrarlaLaDejaCerrada() {
        sesion.cerrar(INICIO.plusSeconds(10));

        assertFalse(sesion.abierta());
    }

    @Test
    void cerrarlaDosVecesNoFallaYSigueCerrada() {
        sesion.cerrar(INICIO.plusSeconds(10));
        sesion.cerrar(INICIO.plusSeconds(20));

        assertFalse(sesion.abierta());
    }

    @Test
    void noEstaInactivaAntesDelLimite() {
        assertFalse(sesion.inactivaDesde(INICIO.plus(TREINTA_MINUTOS), TREINTA_MINUTOS));
    }

    @Test
    void estaInactivaPasadoElLimiteSinActividad() {
        assertTrue(sesion.inactivaDesde(INICIO.plus(TREINTA_MINUTOS).plusSeconds(1), TREINTA_MINUTOS));
    }

    @Test
    void laActividadDentroDelMismoMinutoNoSeRegistra() {
        assertFalse(sesion.registrarActividad(INICIO.plusSeconds(30)));
    }

    @Test
    void laActividadPasadoUnMinutoSeRegistraYAplazaLaInactividad() {
        Instant actividad = INICIO.plus(Duration.ofMinutes(20));

        assertTrue(sesion.registrarActividad(actividad));

        assertFalse(sesion.inactivaDesde(INICIO.plus(Duration.ofMinutes(45)), TREINTA_MINUTOS));
        assertTrue(sesion.inactivaDesde(actividad.plus(TREINTA_MINUTOS).plusSeconds(1), TREINTA_MINUTOS));
    }
}
