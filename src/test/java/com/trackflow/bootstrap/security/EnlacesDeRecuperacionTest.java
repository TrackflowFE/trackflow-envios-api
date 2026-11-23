package com.trackflow.bootstrap.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

class EnlacesDeRecuperacionTest {

    private static final Instant AHORA = Instant.parse("2026-11-01T15:00:00Z");

    private final SolicitudDeClaveRepository solicitudes = mock(SolicitudDeClaveRepository.class);
    private final ClaveProperties propiedades = new ClaveProperties(30, "http://localhost/restablecer", "r@x.co");
    private final EnlacesDeRecuperacion enlaces =
            new EnlacesDeRecuperacion(solicitudes, propiedades, Clock.fixed(AHORA, ZoneOffset.UTC));

    private SolicitudDeClave ultimaGuardada() {
        ArgumentCaptor<SolicitudDeClave> guardada = ArgumentCaptor.forClass(SolicitudDeClave.class);
        verify(solicitudes).save(guardada.capture());
        return guardada.getValue();
    }

    @Test
    void emitirLimpiaLoVencidoYLoPendienteDelUsuarioAntesDeGuardar() {
        enlaces.emitirPara("ana");

        verify(solicitudes).borrarVencidas(AHORA);
        verify(solicitudes).borrarPendientesDe("ana");
    }

    @Test
    void elTokenEmitidoEsSeguroParaUnaUrlYNoSeRepite() {
        String uno = enlaces.emitirPara("ana");
        String otro = enlaces.emitirPara("ana");

        assertTrue(uno.matches("[A-Za-z0-9_-]{43}"));
        assertNotEquals(uno, otro);
    }

    @Test
    void laSolicitudGuardadaVenceSegunLaVigenciaYNoGuardaElTokenEnClaro() {
        String token = enlaces.emitirPara("ana");

        SolicitudDeClave solicitud = ultimaGuardada();
        assertEquals("ana", solicitud.getUsername());
        assertEquals(AHORA.plus(Duration.ofMinutes(30)), solicitud.getExpiresAt());
        assertFalse(solicitud.usada());
        assertTrue(token.length() > 0);
    }

    @Test
    void consumirUnEnlaceVigenteDevuelveAlUsuarioYLoMarcaUsado() {
        String token = enlaces.emitirPara("ana");
        SolicitudDeClave solicitud = ultimaGuardada();
        when(solicitudes.findById(anyString())).thenReturn(Optional.of(solicitud));

        assertEquals("ana", enlaces.consumir(token));
        assertTrue(solicitud.usada());
    }

    @Test
    void unEnlaceNoSePuedeUsarDosVeces() {
        String token = enlaces.emitirPara("ana");
        SolicitudDeClave solicitud = ultimaGuardada();
        when(solicitudes.findById(anyString())).thenReturn(Optional.of(solicitud));
        enlaces.consumir(token);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> enlaces.consumir(token));

        assertEquals(400, error.getStatusCode().value());
        assertEquals(EnlacesDeRecuperacion.ENLACE_INVALIDO, error.getReason());
    }

    @Test
    void unEnlaceDesconocidoSeRechaza() {
        when(solicitudes.findById(anyString())).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> enlaces.consumir("token-que-no-existe"));

        assertEquals(400, error.getStatusCode().value());
        assertEquals(EnlacesDeRecuperacion.ENLACE_INVALIDO, error.getReason());
    }

    @Test
    void unEnlaceVencidoSeRechazaConUnMensajePropio() {
        String token = enlaces.emitirPara("ana");
        SolicitudDeClave solicitud = ultimaGuardada();
        when(solicitudes.findById(anyString())).thenReturn(Optional.of(solicitud));
        EnlacesDeRecuperacion masTarde = new EnlacesDeRecuperacion(solicitudes, propiedades,
                Clock.fixed(AHORA.plus(Duration.ofMinutes(31)), ZoneOffset.UTC));

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> masTarde.consumir(token));

        assertEquals(400, error.getStatusCode().value());
        assertEquals(EnlacesDeRecuperacion.ENLACE_VENCIDO, error.getReason());
        assertFalse(solicitud.usada());
    }
}
