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
        RecogidaResponse response = RecogidaResponse.from(new VistaDeRecogida(pickup, "Medellin", List.of(event), false));
        assertTrue(response.accessRestricted());
        assertNull(response.address());
        assertNull(response.contactName());
        assertNull(response.contactPhone());
        assertNull(response.history().getFirst().detail());
    }
}
