package com.trackflow.modules.pickups.api.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.trackflow.modules.pickups.application.VistaDeRecogida;
import com.trackflow.modules.pickups.domain.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecogidaResponseTest {
    @Test
    void ocultaContactoDireccionYDetalleLibreSinAsignacion() {
        Instant now = Instant.parse("2026-11-16T12:00:00Z");
        Pickup pickup = Pickup.registrar(PickupNumber.of("RC0123456789AB"), "Direccion privada", 1L,
                "Ana Torres", "3001234567", LocalDate.of(2026, 11, 17), "08:00-10:00",
                PackageSize.MEDIUM, BigDecimal.ONE, null, null, "operador", now);
        PickupEvent event = new PickupEvent("e1", "RC0123456789AB", PickupEventType.CANCELLED,
                PickupStatus.REQUESTED, PickupStatus.CANCELLED, "Direccion privada", "operador", now);
        PickupEvent horario = new PickupEvent("e2", "RC0123456789AB", PickupEventType.RESCHEDULED,
                PickupStatus.REQUESTED, PickupStatus.REQUESTED, "De ayer a hoy", "operador", now);
        RecogidaResponse response = RecogidaResponse.from(new VistaDeRecogida(pickup, "Medellin", List.of(event, horario), false));
        assertTrue(response.accessRestricted());
        assertNull(response.address());
        assertNull(response.contactName());
        assertNull(response.contactPhone());
        assertNull(response.history().getFirst().detail());
        assertEquals("De ayer a hoy", response.history().get(1).detail());
    }

    @Test
    void remitenteNoVeIdentidadesNiDetallesLibresDeOperador() {
        Instant now = Instant.parse("2026-11-16T12:00:00Z");
        Pickup pickup = Pickup.registrar(PickupNumber.of("RC0123456789AB"), "Direccion privada", 1L,
                "Ana Torres", "3001234567", LocalDate.of(2026, 11, 17), "08:00-10:00",
                PackageSize.MEDIUM, BigDecimal.ONE, null, "operador.secreto", "creador.secreto", now);
        List<PickupEvent> history = List.of(
                new PickupEvent("e1", "RC0123456789AB", PickupEventType.ASSIGNED,
                        PickupStatus.REQUESTED, PickupStatus.SCHEDULED, "asignada a operador.secreto", "operador.secreto", now),
                new PickupEvent("e2", "RC0123456789AB", PickupEventType.CANCELLED,
                        PickupStatus.SCHEDULED, PickupStatus.CANCELLED, "Motivo de Ana Operadora", "operador.secreto", now),
                new PickupEvent("e3", "RC0123456789AB", PickupEventType.RESCHEDULED,
                        PickupStatus.REQUESTED, PickupStatus.REQUESTED, "Texto libre con Ana Operadora", "operador.secreto", now),
                new PickupEvent("e4", "RC0123456789AB", PickupEventType.REJECTED,
                        PickupStatus.REQUESTED, PickupStatus.REQUESTED, "RECHAZO: Ana Operadora", "operador.secreto", now));
        VistaDeRecogida vista = new VistaDeRecogida(pickup, "Medellin", history, true);

        RecogidaResponse response = RecogidaResponse.paraRemitente(vista);
        assertNull(response.assignedUsername());
        assertEquals("operación", response.requestedBy());
        assertTrue(response.assigned());
        assertEquals("Direccion privada", response.address());
        assertEquals("Ana Torres", response.contactName());
        assertFalse(response.accessRestricted());
        assertEquals(4, response.history().size());
        response.history().forEach(event -> {
            assertEquals("operación", event.actor());
            assertEquals(now, event.occurredAt());
        });
        assertEquals("Asignada a un operador", response.history().getFirst().detail());
        assertNull(response.history().get(1).detail());
        assertNull(response.history().get(2).detail());
        assertEquals("La solicitud no pudo aplicarse", response.history().get(3).detail());
        assertEquals("ASSIGNED", response.history().getFirst().type());
        assertEquals("SCHEDULED", response.history().getFirst().newStatus());
        assertEquals("operador.secreto", RecogidaResponse.from(vista).assignedUsername());
        assertEquals("Motivo de Ana Operadora", RecogidaResponse.from(vista).history().get(1).detail());
    }

    @Test
    void soloExponeHorariosEstructuradosYActorPublico() {
        Instant now = Instant.parse("2026-11-16T12:00:00Z");
        Pickup pickup = Pickup.registrar(PickupNumber.of("RC0123456789AB"), "Direccion", 1L,
                "Ana", "3001234567", LocalDate.of(2026, 11, 17), "08:00-10:00",
                PackageSize.MEDIUM, BigDecimal.ONE, null, null, "remitente", now);
        String detalle = "De 2026-11-17 08:00-10:00 a 2026-11-18 10:00-12:00";
        PickupEvent event = new PickupEvent("e1", "RC0123456789AB", PickupEventType.RESCHEDULED,
                PickupStatus.REQUESTED, PickupStatus.REQUESTED, detalle, "remitente", now);
        RecogidaResponse response = RecogidaResponse.paraRemitente(new VistaDeRecogida(pickup, "Medellin", List.of(event), true));
        assertFalse(response.assigned());
        assertEquals("remitente", response.requestedBy());
        assertEquals("remitente", response.history().getFirst().actor());
        assertEquals(detalle, response.history().getFirst().detail());
    }
}
