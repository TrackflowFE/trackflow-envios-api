package com.trackflow.shared.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class EnmascaradoTest {

    @Test
    void elTelefonoDejaElIndicativoYLosDosUltimosDigitos() {
        assertEquals("300 *** **67", Enmascarado.telefono("3001234567"));
    }

    @Test
    void elTelefonoIgnoraEspaciosYSignos() {
        assertEquals("300 *** **67", Enmascarado.telefono(" 300-123 45.67 "));
    }

    @Test
    void unTelefonoDeCincoDigitosYaSeEnmascaraConElFormatoLargo() {
        assertEquals("123 *** **45", Enmascarado.telefono("12345"));
    }

    @Test
    void unTelefonoMuyCortoSeOcultaPorCompleto() {
        assertEquals("****", Enmascarado.telefono("1234"));
    }

    @Test
    void unTelefonoNuloOSinDigitosDaNulo() {
        assertNull(Enmascarado.telefono(null));
        assertNull(Enmascarado.telefono("sin numero"));
    }

    @Test
    void elDocumentoDejaSoloLosDosUltimos() {
        assertEquals("********90", Enmascarado.documento("1234567890"));
    }

    @Test
    void elDocumentoIgnoraLosEspaciosDeLosExtremos() {
        assertEquals("****90", Enmascarado.documento("  123490 "));
    }

    @Test
    void unDocumentoDeDosCaracteresOMenosSeOcultaPorCompleto() {
        assertEquals("**", Enmascarado.documento("12"));
        assertEquals("*", Enmascarado.documento("1"));
    }

    @Test
    void unDocumentoNuloOEnBlancoDaNulo() {
        assertNull(Enmascarado.documento(null));
        assertNull(Enmascarado.documento("   "));
    }

    @Test
    void laDireccionDejaLosOchoPrimerosCaracteresYOcultaElResto() {
        assertEquals("Cra 45 # ***", Enmascarado.direccion("Cra 45 # 10-20"));
    }

    @Test
    void unaDireccionCortaNoLlevaMarcaDeOcultamiento() {
        assertEquals("Calle 12", Enmascarado.direccion("Calle 12"));
        assertEquals("Calle 5", Enmascarado.direccion("  Calle 5 "));
    }

    @Test
    void unaDireccionNulaOEnBlancoDaNulo() {
        assertNull(Enmascarado.direccion(null));
        assertNull(Enmascarado.direccion(" "));
    }
}
