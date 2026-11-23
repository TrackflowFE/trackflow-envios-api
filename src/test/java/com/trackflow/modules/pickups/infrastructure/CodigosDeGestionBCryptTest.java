package com.trackflow.modules.pickups.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class CodigosDeGestionBCryptTest {

    // Costo mínimo: aquí solo importa el comportamiento, no la fortaleza del hash.
    private final CodigosDeGestionBCrypt codigos = new CodigosDeGestionBCrypt(new BCryptPasswordEncoder(4));

    @Test
    void elCodigoGeneradoTieneOchoCaracteresDelAlfabetoPermitido() {
        for (int i = 0; i < 50; i++) {
            assertTrue(codigos.generar().matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}"));
        }
    }

    @Test
    void dosCodigosGeneradosNoSuelenRepetirse() {
        assertNotEquals(codigos.generar(), codigos.generar());
    }

    @Test
    void elHashNoGuardaElCodigoEnClaro() {
        String codigo = codigos.generar();

        assertNotEquals(codigo, codigos.hash(codigo));
    }

    @Test
    void elCodigoCorrectoCoincideConSuHash() {
        String codigo = codigos.generar();

        assertTrue(codigos.coincide(codigo, codigos.hash(codigo)));
    }

    @Test
    void otroCodigoNoCoincide() {
        String hash = codigos.hash("ABCD2345");

        assertFalse(codigos.coincide("ABCD2346", hash));
    }

    @Test
    void unCodigoConFormatoInvalidoNuncaCoincide() {
        String hash = codigos.hash("ABCD2345");

        assertFalse(codigos.coincide(null, hash));
        assertFalse(codigos.coincide("abcd2345", hash));
        assertFalse(codigos.coincide("ABC", hash));
        assertFalse(codigos.coincide("ABCD2345X", hash));
        assertFalse(codigos.coincide("ABCD234O", hash));
    }

    @Test
    void sinHashGuardadoNuncaCoincide() {
        assertFalse(codigos.coincide("ABCD2345", null));
        assertFalse(codigos.coincide("ABCD2345", ""));
        assertFalse(codigos.coincide("ABCD2345", "   "));
    }

    @Test
    void dosHashesDelMismoCodigoSonDistintosPorLaSal() {
        assertNotEquals(codigos.hash("ABCD2345"), codigos.hash("ABCD2345"));
        assertEquals(60, codigos.hash("ABCD2345").length());
    }
}
