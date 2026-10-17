package com.trackflow.modules.shipments.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ShipmentTest {

    private static final Instant REGISTRO = Instant.parse("2026-10-15T18:00:00Z");

    private final TrackingNumber numero = TrackingNumber.of("TF0123456789AB");
    private final Party remitente = new Party("Ana", TipoDocumento.CC, "1000000001", "300", "Calle 1", 1L);
    private final Party destinatario = new Party("Luis", TipoDocumento.CC, "2000000002", "301", "Carrera 2", 10L);

    private Shipment registrar() {
        return Shipment.registrar(numero, remitente, destinatario, "Documentos", REGISTRO);
    }

    @Test
    void unEnvioRecienRegistradoQuedaEnEstadoRegistradoYSinMovimientos() {
        Shipment envio = registrar();

        assertNull(envio.getId());
        assertSame(numero, envio.getTrackingNumber());
        assertSame(remitente, envio.getSender());
        assertSame(destinatario, envio.getRecipient());
        assertEquals("Documentos", envio.getDescription());
        assertEquals(REGISTRO, envio.getRegisteredAt());
        assertEquals(ShipmentStatus.REGISTERED, envio.getStatus());
        assertNull(envio.getLastMovementPoint());
        assertNull(envio.getLastMovementAt());
    }

    @Test
    void elPrimerMovimientoSeAplica() {
        Shipment envio = registrar();
        Instant movido = REGISTRO.plusSeconds(3600);

        boolean aplicado = envio.aplicarMovimiento(ShipmentStatus.AT_DISTRIBUTION_CENTER, "Centro Medellín", movido);

        assertTrue(aplicado);
        assertEquals(ShipmentStatus.AT_DISTRIBUTION_CENTER, envio.getStatus());
        assertEquals("Centro Medellín", envio.getLastMovementPoint());
        assertEquals(movido, envio.getLastMovementAt());
    }

    @Test
    void unMovimientoPosteriorAlUltimoSeAplica() {
        Shipment envio = registrar();
        envio.aplicarMovimiento(ShipmentStatus.AT_DISTRIBUTION_CENTER, "Centro", REGISTRO.plusSeconds(60));

        boolean aplicado = envio.aplicarMovimiento(ShipmentStatus.IN_TRANSIT, "Vía", REGISTRO.plusSeconds(120));

        assertTrue(aplicado);
        assertEquals(ShipmentStatus.IN_TRANSIT, envio.getStatus());
    }

    @Test
    void unMovimientoConLaMismaFechaDelUltimoTambienSeAplica() {
        Shipment envio = registrar();
        Instant momento = REGISTRO.plusSeconds(60);
        envio.aplicarMovimiento(ShipmentStatus.AT_DISTRIBUTION_CENTER, "Centro", momento);

        assertTrue(envio.aplicarMovimiento(ShipmentStatus.IN_TRANSIT, "Vía", momento));
    }

    @Test
    void unMovimientoReportadoTardeNoHaceRetrocederElEstado() {
        Shipment envio = registrar();
        Instant entrega = REGISTRO.plusSeconds(7200);
        envio.aplicarMovimiento(ShipmentStatus.DELIVERED, "Destino", entrega);

        boolean aplicado = envio.aplicarMovimiento(ShipmentStatus.IN_TRANSIT, "Vía", entrega.minusSeconds(1));

        assertFalse(aplicado);
        assertEquals(ShipmentStatus.DELIVERED, envio.getStatus());
        assertEquals("Destino", envio.getLastMovementPoint());
        assertEquals(entrega, envio.getLastMovementAt());
    }

    @Test
    void losEstadosSiguenElOrdenDelRecorrido() {
        ShipmentStatus[] estados = ShipmentStatus.values();

        assertEquals(6, estados.length);
        assertEquals(ShipmentStatus.REGISTERED, estados[0]);
        assertEquals(ShipmentStatus.DELIVERED, estados[5]);
    }

    @Test
    void laExcepcionDeEnvioInexistenteIncluyeElNumero() {
        ShipmentNotFoundException e = new ShipmentNotFoundException("TF0123456789AB");

        assertEquals("No existe un envío con el número de seguimiento TF0123456789AB", e.getMessage());
    }
}
