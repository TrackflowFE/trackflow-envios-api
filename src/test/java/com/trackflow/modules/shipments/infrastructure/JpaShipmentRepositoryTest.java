package com.trackflow.modules.shipments.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.trackflow.modules.shipments.domain.Party;
import com.trackflow.modules.shipments.domain.Shipment;
import com.trackflow.modules.shipments.domain.TipoDocumento;
import com.trackflow.modules.shipments.domain.TrackingNumber;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** El adaptador delega en Spring Data sin transformar nada. */
class JpaShipmentRepositoryTest {

    private static final TrackingNumber NUMERO = TrackingNumber.of("TF0123456789AB");

    private final ShipmentJpaRepository jpa = mock(ShipmentJpaRepository.class);
    private final JpaShipmentRepository repositorio = new JpaShipmentRepository(jpa);
    private final Shipment envio = Shipment.registrar(NUMERO,
            new Party("Ana", TipoDocumento.CC, "1000000001", "300", "Calle 1", 1L),
            new Party("Luis", TipoDocumento.CC, "2000000002", "301", "Carrera 2", 10L),
            "Documentos", Instant.parse("2026-10-15T18:00:00Z"));

    @Test
    void guardaYDevuelveLoQueGuardoJpa() {
        when(jpa.save(envio)).thenReturn(envio);

        assertSame(envio, repositorio.save(envio));
    }

    @Test
    void listaTodosLosEnvios() {
        when(jpa.findAll()).thenReturn(List.of(envio));

        assertEquals(List.of(envio), repositorio.findAll());
    }

    @Test
    void buscaPorNumeroDeSeguimiento() {
        when(jpa.findByTrackingNumber(NUMERO)).thenReturn(Optional.of(envio));

        assertSame(envio, repositorio.findByTrackingNumber(NUMERO).orElseThrow());
        assertTrue(repositorio.findByTrackingNumber(TrackingNumber.of("TF000000000000")).isEmpty());
    }

    @Test
    void indicaSiElNumeroYaExiste() {
        when(jpa.existsByTrackingNumber(NUMERO)).thenReturn(true);

        assertTrue(repositorio.existsByTrackingNumber(NUMERO));
        assertFalse(repositorio.existsByTrackingNumber(TrackingNumber.of("TF000000000000")));
    }
}
