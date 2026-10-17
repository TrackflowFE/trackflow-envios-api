package com.trackflow.bootstrap.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

class SesionActivaValidatorTest {

    private final Sesiones sesiones = mock(Sesiones.class);
    private final SesionActivaValidator validador = new SesionActivaValidator(sesiones);

    private static Jwt token(String jti) {
        Instant ahora = Instant.parse("2026-10-15T18:00:00Z");
        return Jwt.withTokenValue("t").header("alg", "HS256").subject("operador").jti(jti).issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(60)).build();
    }

    @Test
    void aceptaElTokenSiSuSesionSigueAbierta() {
        when(sesiones.validarYRenovar("jti-1")).thenReturn(true);

        OAuth2TokenValidatorResult resultado = validador.validate(token("jti-1"));

        assertFalse(resultado.hasErrors());
    }

    @Test
    void rechazaElTokenDeUnaSesionTerminadaComoTokenInvalido() {
        when(sesiones.validarYRenovar("jti-2")).thenReturn(false);

        OAuth2TokenValidatorResult resultado = validador.validate(token("jti-2"));

        assertTrue(resultado.hasErrors());
        OAuth2Error error = resultado.getErrors().iterator().next();
        assertEquals("invalid_token", error.getErrorCode());
        assertTrue(error.getDescription().contains("inicie sesión de nuevo"));
    }
}
