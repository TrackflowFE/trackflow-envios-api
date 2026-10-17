package com.trackflow.modules.shipments.infrastructure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trackflow.modules.shipments.application.ShipmentRepository;
import com.trackflow.modules.shipments.domain.TrackingNumber;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UuidTrackingNumberGeneratorTest {

    private final ShipmentRepository shipments = mock(ShipmentRepository.class);
    private final UuidTrackingNumberGenerator generador = new UuidTrackingNumberGenerator(shipments);

    @Test
    void generaUnNumeroConPrefijoTfYDoceCaracteresHexadecimalesEnMayuscula() {
        when(shipments.existsByTrackingNumber(any())).thenReturn(false);

        TrackingNumber numero = generador.next();

        assertTrue(numero.value().matches("TF[0-9A-F]{12}"), numero.value());
        verify(shipments).existsByTrackingNumber(numero);
    }

    @Test
    void siElNumeroYaExisteGeneraOtro() {
        when(shipments.existsByTrackingNumber(any())).thenReturn(true, false);

        TrackingNumber numero = generador.next();

        ArgumentCaptor<TrackingNumber> consultados = ArgumentCaptor.forClass(TrackingNumber.class);
        verify(shipments, times(2)).existsByTrackingNumber(consultados.capture());
        TrackingNumber repetido = consultados.getAllValues().get(0);
        assertNotEquals(repetido, numero);
        assertFalse(numero.equals(repetido));
    }

    @Test
    void dosLlamadasDanNumerosDistintos() {
        when(shipments.existsByTrackingNumber(any())).thenReturn(false);

        assertNotEquals(generador.next(), generador.next());
    }
}
