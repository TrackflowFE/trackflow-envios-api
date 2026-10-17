package com.trackflow.modules.shipments.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trackflow.modules.shipments.domain.Party;
import com.trackflow.modules.shipments.domain.Shipment;
import com.trackflow.modules.shipments.domain.ShipmentStatus;
import com.trackflow.modules.shipments.domain.TipoDocumento;
import com.trackflow.modules.shipments.domain.TrackingNumber;
import com.trackflow.shared.events.EnvioCreadoEvent;
import com.trackflow.shared.events.EventPublisher;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import com.trackflow.shared.geografia.Ciudad;
import com.trackflow.shared.geografia.CiudadDesconocidaException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RepublicarEnviosCreadosTest {

    private static final Instant AHORA = Instant.parse("2026-10-16T08:00:00Z");
    private static final Instant REGISTRO = Instant.parse("2026-10-15T18:00:00Z");

    private final ShipmentRepository shipments = mock(ShipmentRepository.class);
    private final EventPublisher events = mock(EventPublisher.class);
    private final CatalogoDeCiudades ciudades = mock(CatalogoDeCiudades.class);
    private final RepublicarEnviosCreados republicar = new RepublicarEnviosCreados(shipments, events, ciudades,
            Clock.fixed(AHORA, ZoneOffset.UTC));

    @BeforeEach
    void catalogo() {
        when(ciudades.exigir(any())).thenCallRealMethod();
        when(ciudades.porId(1L)).thenReturn(Optional.of(new Ciudad(1L, "MEDELLÍN", "ANTIOQUIA")));
        when(ciudades.porId(10L)).thenReturn(Optional.of(new Ciudad(10L, "BOGOTÁ D.C.", "BOGOTÁ")));
    }

    private static Shipment envio(String numero, Long destino) {
        return Shipment.registrar(TrackingNumber.of(numero),
                new Party("Ana", TipoDocumento.CC, "1000000001", "300", "Calle 1", 1L),
                new Party("Luis", TipoDocumento.CC, "2000000002", "301", "Carrera 2", destino),
                "Documentos", REGISTRO);
    }

    @Test
    void publicaUnEventoPorCadaEnvioConSuEstadoActual() {
        Shipment entregado = envio("TF000000000002", 10L);
        entregado.aplicarMovimiento(ShipmentStatus.DELIVERED, "Destino", REGISTRO.plusSeconds(3600));
        when(shipments.findAll()).thenReturn(List.of(envio("TF000000000001", 10L), entregado));

        int republicados = republicar.ejecutar();

        assertEquals(2, republicados);
        ArgumentCaptor<EnvioCreadoEvent> eventos = ArgumentCaptor.forClass(EnvioCreadoEvent.class);
        verify(events, times(2)).publish(eventos.capture());
        EnvioCreadoEvent primero = eventos.getAllValues().get(0);
        assertEquals("TF000000000001", primero.trackingNumber());
        assertEquals("REGISTERED", primero.status());
        assertEquals("Ana", primero.senderName());
        assertEquals(1L, primero.originCityId());
        assertEquals("MEDELLÍN - ANTIOQUIA", primero.originCity());
        assertEquals("Luis", primero.recipientName());
        assertEquals("Carrera 2", primero.recipientAddress());
        assertEquals(10L, primero.destinationCityId());
        assertEquals("BOGOTÁ D.C. - BOGOTÁ", primero.destinationCity());
        assertEquals(REGISTRO, primero.registeredAt());
        assertEquals(AHORA, primero.occurredAt());
        assertEquals("DELIVERED", eventos.getAllValues().get(1).status());
    }

    @Test
    void sinEnviosNoPublicaNada() {
        when(shipments.findAll()).thenReturn(List.of());

        assertEquals(0, republicar.ejecutar());
        verifyNoInteractions(events);
    }

    @Test
    void fallaSiUnEnvioApuntaAUnaCiudadQueYaNoEstaEnElCatalogo() {
        when(ciudades.porId(99L)).thenReturn(Optional.empty());
        when(shipments.findAll()).thenReturn(List.of(envio("TF000000000001", 99L)));

        assertThrows(CiudadDesconocidaException.class, republicar::ejecutar);
        verifyNoInteractions(events);
    }
}
