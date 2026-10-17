package com.trackflow.modules.shipments.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Validación del número según el tipo de documento, incluido el dígito de verificación del NIT. */
class TipoDocumentoTest {

    @ParameterizedTest
    @CsvSource({
            "CC, 123456",
            "CC, 1000000001",
            "CE, 9876543",
            "TI, 1234567890",
            "TI, 12345678901",
            "PP, AB12345",
            "NIT, 900123456",
            "NIT, 9001234567",
            "NIT, 900123456-8"
    })
    void aceptaLosNumerosConElFormatoDeSuTipo(TipoDocumento tipo, String numero) {
        assertTrue(tipo.aceptaNumero(numero));
    }

    @ParameterizedTest
    @CsvSource({
            "CC, 12345",
            "CC, 12345678901",
            "CC, 12A456",
            "CE, 12345",
            "TI, 123456789",
            "PP, AB-12",
            "PP, ab12345",
            "NIT, 90012345",
            "NIT, 900123456-",
            "NIT, 900123456-12"
    })
    void rechazaLosNumerosQueNoTienenElFormatoDeSuTipo(TipoDocumento tipo, String numero) {
        assertFalse(tipo.aceptaNumero(numero));
    }

    @Test
    void ningunTipoAceptaUnNumeroNulo() {
        for (TipoDocumento tipo : TipoDocumento.values()) {
            assertFalse(tipo.aceptaNumero(null), tipo.name());
        }
    }

    @Test
    void unNitConDigitoDeVerificacionIncorrectoSeRechaza() {
        assertFalse(TipoDocumento.NIT.aceptaNumero("900123456-7"));
    }

    @Test
    void unNitConResiduoMenorQueDosUsaElResiduoComoDigito() {
        // 100000000: suma = 1 * 41 = 41, residuo 41 % 11 = 8 -> dígito 3.
        // 800000000: suma = 8 * 41 = 328, residuo 328 % 11 = 9 -> dígito 2.
        // 270000000: suma = 2*41 + 7*37 = 341, residuo 0 -> dígito 0.
        assertTrue(TipoDocumento.NIT.aceptaNumero("100000000-3"));
        assertTrue(TipoDocumento.NIT.aceptaNumero("800000000-2"));
        assertTrue(TipoDocumento.NIT.aceptaNumero("270000000-0"));
        assertFalse(TipoDocumento.NIT.aceptaNumero("270000000-1"));
    }

    @Test
    void soloElNitEsEmpresarial() {
        assertTrue(TipoDocumento.NIT.esEmpresarial());
        assertFalse(TipoDocumento.CC.esEmpresarial());
        assertFalse(TipoDocumento.CE.esEmpresarial());
        assertFalse(TipoDocumento.TI.esEmpresarial());
        assertFalse(TipoDocumento.PP.esEmpresarial());
    }

    @Test
    void cadaTipoTieneDescripcionYFormatoEsperado() {
        assertEquals("Cédula de ciudadanía", TipoDocumento.CC.descripcion());
        assertEquals("Pasaporte", TipoDocumento.PP.descripcion());
        assertEquals("entre 6 y 10 dígitos", TipoDocumento.CC.formatoEsperado());
        assertEquals("entre 6 y 10 dígitos", TipoDocumento.CE.formatoEsperado());
        assertEquals("entre 10 y 11 dígitos", TipoDocumento.TI.formatoEsperado());
        assertEquals("entre 5 y 15 caracteres, letras mayúsculas y dígitos", TipoDocumento.PP.formatoEsperado());
        assertTrue(TipoDocumento.NIT.formatoEsperado().contains("dígito de verificación"));
    }
}
