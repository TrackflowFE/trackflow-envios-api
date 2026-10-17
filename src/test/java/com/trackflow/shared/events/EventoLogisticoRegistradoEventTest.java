package com.trackflow.shared.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Las formas anteriores del contrato siguen construyendo el evento con los campos nuevos vacíos. */
class EventoLogisticoRegistradoEventTest {

    private static final Instant MOVIDO = Instant.parse("2026-10-15T17:00:00Z");
    private static final Instant REGISTRADO = Instant.parse("2026-10-15T17:30:00Z");
    private static final Instant PUBLICADO = Instant.parse("2026-10-15T18:00:00Z");

    @Test
    void sinDatosDeDespachoLosDejaNulos() {
        EventoLogisticoRegistradoEvent evento = new EventoLogisticoRegistradoEvent(7L, "TF0123456789AB",
                "RECEPCION", "AT_DISTRIBUTION_CENTER", "Centro", 3L, 1L, "MEDELLÍN", MOVIDO, REGISTRADO, PUBLICADO);

        assertEquals(1L, evento.cityId());
        assertEquals("MEDELLÍN", evento.cityName());
        assertEquals(MOVIDO, evento.movedAt());
        assertEquals(REGISTRADO, evento.registeredAt());
        assertEquals(PUBLICADO, evento.occurredAt());
        assertNull(evento.nextStopCityId());
        assertNull(evento.nextStopCity());
        assertNull(evento.plannedLegHours());
    }

    @Test
    void sinIdDeCiudadLoDejaNuloYConservaElResto() {
        EventoLogisticoRegistradoEvent evento = new EventoLogisticoRegistradoEvent(7L, "TF0123456789AB",
                "RECEPCION", "AT_DISTRIBUTION_CENTER", "Centro", 3L, "MEDELLÍN", MOVIDO, REGISTRADO, PUBLICADO);

        assertNull(evento.cityId());
        assertEquals(3L, evento.centerId());
        assertEquals("MEDELLÍN", evento.cityName());
        assertEquals("AT_DISTRIBUTION_CENTER", evento.resultingStatus());
        assertNull(evento.plannedLegHours());
    }

    @Test
    void unDespachoLlevaLaSiguienteParadaYLaDuracionDelTramo() {
        EventoLogisticoRegistradoEvent evento = new EventoLogisticoRegistradoEvent(8L, "TF0123456789AB",
                "DESPACHO", "IN_TRANSIT", "Centro", 3L, 1L, "MEDELLÍN", MOVIDO, REGISTRADO, PUBLICADO, 10L,
                "BOGOTÁ D.C.", 9);

        assertEquals(10L, evento.nextStopCityId());
        assertEquals("BOGOTÁ D.C.", evento.nextStopCity());
        assertEquals(9, evento.plannedLegHours());
    }
}
