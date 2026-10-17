package com.trackflow.modules.shipments.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PartyTest {

    @Test
    void guardaLosDatosDeLaPersona() {
        Party party = new Party("Ana Torres", TipoDocumento.CC, "1000000001", "3000000001", "Calle 1 # 1-1", 1L);

        assertEquals("Ana Torres", party.getFullName());
        assertEquals(TipoDocumento.CC, party.getDocumentType());
        assertEquals("1000000001", party.getDocumentNumber());
        assertEquals("3000000001", party.getPhone());
        assertEquals("Calle 1 # 1-1", party.getAddress());
        assertEquals(1L, party.getCityId());
    }

    @Test
    void normalizaElNumeroDeDocumentoQuitandoEspaciosYPasandoAMayusculas() {
        Party party = new Party("Luis", TipoDocumento.PP, "  ab12345 ", "300", "Calle", 2L);

        assertEquals("AB12345", party.getDocumentNumber());
    }

    @Test
    void rechazaUnNumeroQueNoCorrespondeAlTipo() {
        DocumentoInvalidoException e = assertThrows(DocumentoInvalidoException.class,
                () -> new Party("Ana", TipoDocumento.CC, "12AB", "300", "Calle", 1L));

        assertEquals(TipoDocumento.CC, e.getTipo());
        assertEquals("12AB", e.getNumero());
        assertTrue(e.getMessage().contains("entre 6 y 10 dígitos"));
    }

    @Test
    void rechazaLaPersonaSinTipoDeDocumento() {
        DocumentoInvalidoException e = assertThrows(DocumentoInvalidoException.class,
                () -> new Party("Ana", null, "1000000001", "300", "Calle", 1L));

        assertNull(e.getTipo());
        assertEquals("Falta el tipo de documento: debe ser CC, CE, TI, PP o NIT", e.getMessage());
    }

    @Test
    void rechazaLaPersonaSinNumeroDeDocumento() {
        DocumentoInvalidoException e = assertThrows(DocumentoInvalidoException.class,
                () -> new Party("Ana", TipoDocumento.CC, null, "300", "Calle", 1L));

        assertNull(e.getNumero());
    }
}
