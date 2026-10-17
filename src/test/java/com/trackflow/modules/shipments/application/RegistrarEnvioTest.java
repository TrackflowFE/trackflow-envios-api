package com.trackflow.modules.shipments.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import com.trackflow.shared.geografia.Ciudad;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RegistrarEnvioTest {

    private static final Instant AHORA = Instant.parse("2026-10-15T18:30:00Z");
    private static final Instant SOLICITADO = Instant.parse("2026-10-15T18:00:00Z");
    private static final TrackingNumber NUMERO = TrackingNumber.of("TF0123456789AB");

    private final ShipmentRepository shipments = mock(ShipmentRepository.class);
    private final EventPublisher events = mock(EventPublisher.class);
    private final RegistrarEnvio registrar = new RegistrarEnvio(shipments, events, Clock.fixed(AHORA, ZoneOffset.UTC));

    private EnvioSolicitado solicitud(Instant solicitadoEn) {
        return new EnvioSolicitado("evt-1", "TF0123456789AB",
                new Party("Ana", TipoDocumento.CC, "1000000001", "300", "Calle 1", 1L),
                new Party("Luis", TipoDocumento.CC, "2000000002", "301", "Carrera 2", 10L),
                new Ciudad(1L, "MEDELLÍN", "ANTIOQUIA"),
                new Ciudad(10L, "BOGOTÁ D.C.", "BOGOTÁ"),
                "Documentos",
                solicitadoEn);
    }

    @Test
    void registraElEnvioConLaFechaDeAdmisionYPublicaQueSeCreo() {
        when(shipments.existsByTrackingNumber(NUMERO)).thenReturn(false);
        when(shipments.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        Optional<Shipment> registrado = registrar.ejecutar(solicitud(SOLICITADO));

        assertTrue(registrado.isPresent());
        Shipment envio = registrado.get();
        assertEquals(NUMERO, envio.getTrackingNumber());
        assertEquals(ShipmentStatus.REGISTERED, envio.getStatus());
        assertEquals(SOLICITADO, envio.getRegisteredAt());
        assertEquals("Documentos", envio.getDescription());

        ArgumentCaptor<EnvioCreadoEvent> evento = ArgumentCaptor.forClass(EnvioCreadoEvent.class);
        verify(events).publish(evento.capture());
        EnvioCreadoEvent creado = evento.getValue();
        assertEquals("TF0123456789AB", creado.trackingNumber());
        assertEquals("REGISTERED", creado.status());
        assertEquals("Ana", creado.senderName());
        assertEquals(1L, creado.originCityId());
        assertEquals("MEDELLÍN - ANTIOQUIA", creado.originCity());
        assertEquals("Luis", creado.recipientName());
        assertEquals("Carrera 2", creado.recipientAddress());
        assertEquals(10L, creado.destinationCityId());
        assertEquals("BOGOTÁ D.C. - BOGOTÁ", creado.destinationCity());
        assertEquals(SOLICITADO, creado.registeredAt());
        assertEquals(AHORA, creado.occurredAt());
    }

    @Test
    void unMensajeSinFechaDeSolicitudSeRegistraConLaHoraDeConsumo() {
        when(shipments.existsByTrackingNumber(NUMERO)).thenReturn(false);
        when(shipments.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        Optional<Shipment> registrado = registrar.ejecutar(solicitud(null));

        assertEquals(AHORA, registrado.orElseThrow().getRegisteredAt());
    }

    @Test
    void unaReentregaDelMismoEnvioSeDescartaSinGuardarNiPublicar() {
        when(shipments.existsByTrackingNumber(NUMERO)).thenReturn(true);

        Optional<Shipment> registrado = registrar.ejecutar(solicitud(SOLICITADO));

        assertTrue(registrado.isEmpty());
        verify(shipments, never()).save(any());
        verifyNoInteractions(events);
    }
}
