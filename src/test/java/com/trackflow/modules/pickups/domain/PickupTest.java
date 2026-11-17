package com.trackflow.modules.pickups.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PickupTest {
    private static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");

    private Pickup pickup() {
        return Pickup.registrar(PickupNumber.of("RC0123456789AB"), "Calle 1 # 1-1", 1L,
                "Ana Torres", "3001234567", LocalDate.of(2026, 11, 17), "08:00-10:00",
                PackageSize.MEDIUM, new BigDecimal("5.50"), null, null, "operador", NOW);
    }

    @Test
    void asignacionYTransicionesDeCustodia() {
        Pickup p = pickup();
        assertEquals(PickupStatus.REQUESTED, p.getStatus());
        assertFalse(p.puedeVerDatosPrivados(null));
        p.reprogramar(LocalDate.of(2026, 11, 17), "08:00-10:00", "operador", NOW);
        assertEquals(PickupStatus.SCHEDULED, p.getStatus());
        assertTrue(p.puedeVerDatosPrivados("operador"));
        assertFalse(p.puedeVerDatosPrivados("admin"));
        p.despachar(NOW);
        p.completar(NOW);
        assertEquals(PickupStatus.PICKED_UP, p.getStatus());
        assertThrows(TransicionDeRecogidaInvalidaException.class, () -> p.cancelar(NOW));
        p.vincularEnvio("TF0123456789AB", NOW);
        assertThrows(IllegalStateException.class, () -> p.vincularEnvio("TF0123456789AC", NOW));
    }

    @Test
    void cancelarEsTerminal() {
        Pickup p = pickup();
        p.cancelar(NOW);
        assertThrows(TransicionDeRecogidaInvalidaException.class, () -> p.despachar(NOW));
        assertThrows(TransicionDeRecogidaInvalidaException.class, () -> p.vincularEnvio("TF0123456789AB", NOW));
    }

    @Test
    void reprogramarSinCambiosSeRechazaYDistingueAsignacion() {
        Pickup p = pickup();
        assertThrows(ReprogramacionSinCambiosException.class,
                () -> p.reprogramar(p.getScheduledDate(), p.getTimeSlot(), null, NOW));
        assertEquals(PickupEventType.RESCHEDULED,
                p.reprogramar(LocalDate.of(2026, 11, 18), "08:00-10:00", null, NOW));
        assertEquals(PickupStatus.REQUESTED, p.getStatus());
        assertEquals(PickupEventType.ASSIGNED,
                p.reprogramar(p.getScheduledDate(), p.getTimeSlot(), "operador", NOW));
        assertEquals(PickupEventType.RESCHEDULED,
                p.reprogramar(LocalDate.of(2026, 11, 19), "08:00-10:00", null, NOW));
        assertEquals("operador", p.getAssignedUsername());
    }
}
