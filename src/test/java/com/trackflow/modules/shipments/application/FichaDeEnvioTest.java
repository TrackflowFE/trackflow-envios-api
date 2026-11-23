package com.trackflow.modules.shipments.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.trackflow.modules.shipments.domain.Party;
import com.trackflow.modules.shipments.domain.Shipment;
import com.trackflow.modules.shipments.domain.TipoDocumento;
import com.trackflow.modules.shipments.domain.TrackingNumber;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import com.trackflow.shared.geografia.Ciudad;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FichaDeEnvioTest {

    private static final Instant REGISTRO = Instant.parse("2026-10-15T18:00:00Z");

    private final CatalogoDeCiudades ciudades = mock(CatalogoDeCiudades.class);
    private final Party remitente =
            new Party("Ana Gómez", TipoDocumento.CC, "1000000001", "3001234567", "Cra 45 # 10-20", 1L);
    private final Party destinatario =
            new Party("Luis Pérez", TipoDocumento.CC, "2000000002", "3109876543", "Calle 5", 10L);
    private final Shipment envio = Shipment.registrar(TrackingNumber.of("TF0123456789AB"), remitente, destinatario,
            "Documentos", REGISTRO);

    private void catalogo() {
        when(ciudades.porId(1L)).thenReturn(Optional.of(new Ciudad(1L, "Medellín", "Antioquia")));
        when(ciudades.porId(10L)).thenReturn(Optional.of(new Ciudad(10L, "Bogotá", "Cundinamarca")));
    }

    @Test
    void laFichaEnmascaradaOcultaDocumentoTelefonoYDireccion() {
        catalogo();

        FichaDeEnvio ficha = FichaDeEnvio.enmascarada(envio, ciudades);

        assertFalse(ficha.datosCompletos());
        assertEquals("TF0123456789AB", ficha.trackingNumber());
        assertEquals("REGISTERED", ficha.status());
        assertEquals(REGISTRO, ficha.registeredAt());
        assertEquals("Documentos", ficha.description());
        assertEquals("Ana Gómez", ficha.sender().name());
        assertEquals("CC", ficha.sender().documentType());
        assertEquals("********01", ficha.sender().documentNumber());
        assertEquals("300 *** **67", ficha.sender().phone());
        assertEquals("Cra 45 # ***", ficha.sender().address());
        assertEquals("Medellín", ficha.sender().city());
        assertEquals("Bogotá", ficha.recipient().city());
        assertEquals("Calle 5", ficha.recipient().address());
    }

    @Test
    void laFichaCompletaMuestraLosDatosTalComoEstanYSeMarcaComoCompleta() {
        catalogo();

        FichaDeEnvio ficha = FichaDeEnvio.completa(envio, ciudades);

        assertTrue(ficha.datosCompletos());
        assertEquals("1000000001", ficha.sender().documentNumber());
        assertEquals("3001234567", ficha.sender().phone());
        assertEquals("Cra 45 # 10-20", ficha.sender().address());
        assertEquals("2000000002", ficha.recipient().documentNumber());
        assertEquals("3109876543", ficha.recipient().phone());
    }

    @Test
    void sinLaCiudadEnElCatalogoLaFichaLaDejaVacia() {
        FichaDeEnvio ficha = FichaDeEnvio.completa(envio, ciudades);

        assertNull(ficha.sender().city());
        assertNull(ficha.recipient().city());
    }
}
