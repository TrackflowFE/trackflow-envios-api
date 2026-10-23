package com.trackflow.modules.shipments.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * BUG-07: un NIT de 10 dígitos con dígito de verificación respondía 500 porque la tabla
 * de pesos de la DIAN solo tenía 9 valores. Ahora se valida y, si el dígito no coincide,
 * se rechaza como cualquier otro documento inválido.
 */
class TipoDocumentoNitDiezDigitosTest {

    @ParameterizedTest
    @ValueSource(strings = { "1234567890-2", "1017254893-3" })
    void aceptaUnNitDeDiezDigitosConSuDigitoCorrecto(String nit) {
        assertTrue(TipoDocumento.NIT.aceptaNumero(nit));
    }

    @ParameterizedTest
    @ValueSource(strings = { "1234567890-0", "1234567890-1", "1017254893-4" })
    void rechazaUnNitDeDiezDigitosConUnDigitoIncorrecto(String nit) {
        assertFalse(TipoDocumento.NIT.aceptaNumero(nit));
    }

    @Test
    void aceptaUnNitDeDiezDigitosSinDigitoDeVerificacion() {
        assertTrue(TipoDocumento.NIT.aceptaNumero("1234567890"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "890903938-8", "899999068-1", "860034313-7", "800197268-4", "900123456-8" })
    void sigueAceptandoNitDeNueveDigitosConSuDigitoCorrecto(String nit) {
        assertTrue(TipoDocumento.NIT.aceptaNumero(nit));
    }

    @Test
    void sigueRechazandoUnNitDeNueveDigitosConUnDigitoIncorrecto() {
        assertFalse(TipoDocumento.NIT.aceptaNumero("900123456-0"));
    }

    @Test
    void unRemitenteConNitDeDiezDigitosCorrectoSeCrea() {
        assertDoesNotThrow(() -> new Party("Transportes Andinos SAS", TipoDocumento.NIT, "1234567890-2",
                "3000000001", "Calle 1 # 1-1", 1L));
    }

    @Test
    void unRemitenteConNitDeDiezDigitosIncorrectoSeRechazaComoDocumentoInvalido() {
        assertThrows(DocumentoInvalidoException.class, () -> new Party("Transportes Andinos SAS", TipoDocumento.NIT,
                "1234567890-1", "3000000001", "Calle 1 # 1-1", 1L));
    }
}
