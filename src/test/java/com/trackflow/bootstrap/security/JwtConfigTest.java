package com.trackflow.bootstrap.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;

class JwtConfigTest {

    private static final String SECRETO = "0123456789abcdef0123456789abcdef";

    private final Sesiones sesiones = mock(Sesiones.class);

    private static JwtConfig con(String secreto) {
        return new JwtConfig(new SecurityProperties(secreto, "operador", "clave", "admin", "", 480, 30));
    }

    private static String firmar(JwtEncoder encoder, String jti, Instant expira) {
        JwtClaimsSet claims = JwtClaimsSet.builder().id(jti).subject("operador").issuedAt(expira.minusSeconds(60))
                .expiresAt(expira).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    @Test
    void elCodificadorDeClavesUsaBcrypt() {
        PasswordEncoder encoder = con(SECRETO).passwordEncoder();

        String hash = encoder.encode("secreta");

        assertTrue(hash.startsWith("$2"));
        assertTrue(encoder.matches("secreta", hash));
    }

    @Test
    void unTokenFirmadoConLaMismaClaveYConSesionAbiertaSeAcepta() {
        JwtConfig config = con(SECRETO);
        when(sesiones.validarYRenovar("jti-1")).thenReturn(true);

        String token = firmar(config.jwtEncoder(), "jti-1", Instant.now().plusSeconds(300));

        assertEquals("operador", config.jwtDecoder(sesiones).decode(token).getSubject());
    }

    @Test
    void unTokenDeUnaSesionCerradaSeRechazaAunqueNoHayaVencido() {
        JwtConfig config = con(SECRETO);
        when(sesiones.validarYRenovar("jti-1")).thenReturn(false);
        String token = firmar(config.jwtEncoder(), "jti-1", Instant.now().plusSeconds(300));
        JwtDecoder decoder = config.jwtDecoder(sesiones);

        assertThrows(JwtValidationException.class, () -> decoder.decode(token));
    }

    @Test
    void unTokenVencidoSeRechaza() {
        JwtConfig config = con(SECRETO);
        when(sesiones.validarYRenovar("jti-1")).thenReturn(true);
        String token = firmar(config.jwtEncoder(), "jti-1", Instant.now().minusSeconds(3600));
        JwtDecoder decoder = config.jwtDecoder(sesiones);

        assertThrows(JwtValidationException.class, () -> decoder.decode(token));
    }

    @Test
    void unTokenFirmadoConOtroSecretoSeRechaza() {
        String token = firmar(con("otro-secreto-de-al-menos-32-caracteres").jwtEncoder(), "jti-1",
                Instant.now().plusSeconds(300));
        JwtDecoder decoder = con(SECRETO).jwtDecoder(sesiones);

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }
}
