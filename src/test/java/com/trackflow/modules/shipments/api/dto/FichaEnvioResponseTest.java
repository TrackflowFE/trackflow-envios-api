package com.trackflow.modules.shipments.api.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trackflow.modules.shipments.application.FichaDeEnvio;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FichaEnvioResponseTest {

    @Test
    void laRespuestaCopiaTodosLosCamposDeLaFicha() {
        Instant registro = Instant.parse("2026-10-15T18:00:00Z");
        FichaDeEnvio ficha = new FichaDeEnvio("TF0123456789AB", "REGISTERED", registro, "Documentos",
                new FichaDeEnvio.Persona("Ana", "CC", "1000000001", "3001234567", "Calle 1", "Medellín"),
                new FichaDeEnvio.Persona("Luis", "CE", "2000000002", "3109876543", "Carrera 2", "Bogotá"),
                true);

        FichaEnvioResponse respuesta = FichaEnvioResponse.from(ficha);

        assertEquals("TF0123456789AB", respuesta.trackingNumber());
        assertEquals("REGISTERED", respuesta.status());
        assertEquals(registro, respuesta.registeredAt());
        assertEquals("Documentos", respuesta.description());
        assertEquals("Ana", respuesta.sender().name());
        assertEquals("CC", respuesta.sender().documentType());
        assertEquals("1000000001", respuesta.sender().documentNumber());
        assertEquals("3001234567", respuesta.sender().phone());
        assertEquals("Calle 1", respuesta.sender().address());
        assertEquals("Medellín", respuesta.sender().city());
        assertEquals("Luis", respuesta.recipient().name());
        assertEquals("Bogotá", respuesta.recipient().city());
        assertTrue(respuesta.datosCompletos());
    }
}
