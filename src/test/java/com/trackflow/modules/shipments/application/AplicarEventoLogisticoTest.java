package com.trackflow.modules.shipments.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trackflow.modules.shipments.domain.Party;
import com.trackflow.modules.shipments.domain.Shipment;
import com.trackflow.modules.shipments.domain.ShipmentNotFoundException;
import com.trackflow.modules.shipments.domain.ShipmentStatus;
import com.trackflow.modules.shipments.domain.TipoDocumento;
import com.trackflow.modules.shipments.domain.TrackingNumber;
import com.trackflow.shared.events.EventoLogisticoRegistradoEvent;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AplicarEventoLogisticoTest {

    private static final Instant REGISTRO = Instant.parse("2026-10-15T18:00:00Z");
    private static final TrackingNumber NUMERO = TrackingNumber.of("TF0123456789AB");

    private final ShipmentRepository shipments = mock(ShipmentRepository.class);
    private final AplicarEventoLogistico aplicar = new AplicarEventoLogistico(shipments);

    private final Shipment envio = Shipment.registrar(NUMERO,
            new Party("Ana", TipoDocumento.CC, "1000000001", "300", "Calle 1", 1L),
            new Party("Luis", TipoDocumento.CC, "2000000002", "301", "Carrera 2", 10L),
            "Documentos", REGISTRO);

    private static EventoLogisticoRegistradoEvent evento(String estado, Instant movido) {
        return new EventoLogisticoRegistradoEvent(7L, "TF0123456789AB", "RECEPCION", estado, "Centro Medellín",
                3L, 1L, "MEDELLÍN", movido, movido, movido);
    }

    @Test
    void aplicaElMovimientoYGuardaElEnvio() {
        when(shipments.findByTrackingNumber(NUMERO)).thenReturn(Optional.of(envio));
        Instant movido = REGISTRO.plusSeconds(600);

        aplicar.ejecutar(evento("AT_DISTRIBUTION_CENTER", movido));

        assertEquals(ShipmentStatus.AT_DISTRIBUTION_CENTER, envio.getStatus());
        assertEquals("Centro Medellín", envio.getLastMovementPoint());
        assertEquals(movido, envio.getLastMovementAt());
        verify(shipments).save(envio);
    }

    @Test
    void unMovimientoAnteriorAlUltimoNoSeGuarda() {
        envio.aplicarMovimiento(ShipmentStatus.DELIVERED, "Destino", REGISTRO.plusSeconds(7200));
        when(shipments.findByTrackingNumber(NUMERO)).thenReturn(Optional.of(envio));

        aplicar.ejecutar(evento("IN_TRANSIT", REGISTRO.plusSeconds(600)));

        assertEquals(ShipmentStatus.DELIVERED, envio.getStatus());
        verify(shipments, never()).save(any());
    }

    @Test
    void fallaSiElEnvioNoExiste() {
        when(shipments.findByTrackingNumber(NUMERO)).thenReturn(Optional.empty());

        ShipmentNotFoundException e = assertThrows(ShipmentNotFoundException.class,
                () -> aplicar.ejecutar(evento("IN_TRANSIT", REGISTRO)));

        assertEquals("No existe un envío con el número de seguimiento TF0123456789AB", e.getMessage());
        verify(shipments, never()).save(any());
    }

    @Test
    void fallaConUnEstadoQueNoExiste() {
        when(shipments.findByTrackingNumber(NUMERO)).thenReturn(Optional.of(envio));

        assertThrows(IllegalArgumentException.class, () -> aplicar.ejecutar(evento("PERDIDO", REGISTRO)));

        assertEquals(ShipmentStatus.REGISTERED, envio.getStatus());
        verify(shipments, never()).save(any());
    }

    @Test
    void fallaConUnNumeroDeSeguimientoMalFormado() {
        EventoLogisticoRegistradoEvent malo = new EventoLogisticoRegistradoEvent(7L, "NO-VALIDO", "RECEPCION",
                "IN_TRANSIT", "Centro", 3L, 1L, "MEDELLÍN", REGISTRO, REGISTRO, REGISTRO);

        assertThrows(IllegalArgumentException.class, () -> aplicar.ejecutar(malo));
    }
}
