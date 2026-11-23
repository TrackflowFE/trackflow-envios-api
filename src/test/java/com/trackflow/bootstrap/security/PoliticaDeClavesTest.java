package com.trackflow.bootstrap.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class PoliticaDeClavesTest {

    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final PoliticaDeClaves politica = new PoliticaDeClaves(encoder);

    @Test
    void unaClaveNulaSeRechazaPorCorta() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> politica.exigir(null));

        assertEquals(400, error.getStatusCode().value());
        assertEquals(PoliticaDeClaves.MUY_CORTA, error.getReason());
    }

    @Test
    void unaClaveConUnCaracterMenosQueElMinimoSeRechaza() {
        String corta = "a".repeat(PoliticaDeClaves.MINIMO - 1);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> politica.exigir(corta));

        assertEquals(PoliticaDeClaves.MUY_CORTA, error.getReason());
        verifyNoInteractions(encoder);
    }

    @Test
    void unaClaveConLaLongitudMinimaSeAcepta() {
        String justa = "a".repeat(PoliticaDeClaves.MINIMO);

        assertDoesNotThrow(() -> politica.exigir(justa));
    }

    @Test
    void sinClaveAnteriorNoSeCompara() {
        assertDoesNotThrow(() -> politica.exigir("una-clave-larga", null));

        verifyNoInteractions(encoder);
    }

    @Test
    void unaClaveIgualALaAnteriorSeRechaza() {
        when(encoder.matches("una-clave-larga", "hash-vigente")).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> politica.exigir("una-clave-larga", "hash-vigente"));

        assertEquals(400, error.getStatusCode().value());
        assertEquals(PoliticaDeClaves.IGUAL_A_LA_ANTERIOR, error.getReason());
    }

    @Test
    void unaClaveDistintaDeLaAnteriorSeAcepta() {
        when(encoder.matches("otra-clave-larga", "hash-vigente")).thenReturn(false);

        assertDoesNotThrow(() -> politica.exigir("otra-clave-larga", "hash-vigente"));
    }
}
