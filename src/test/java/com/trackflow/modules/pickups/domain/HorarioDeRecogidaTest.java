package com.trackflow.modules.pickups.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HorarioDeRecogidaTest {
    private final HorarioDeRecogida horario = new HorarioDeRecogida(ZoneId.of("America/Bogota"),
            Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
            List.of("08:00-10:00", "10:00-12:00"), Duration.ofHours(4), 30);
    private final Instant now = Instant.parse("2026-11-16T12:00:00Z");

    @Test
    void validaFechaFranjaYAnticipacion() {
        assertThrows(FechaDeRecogidaEnElPasadoException.class,
                () -> horario.exigir(LocalDate.of(2026, 11, 15), "08:00-10:00", now));
        assertThrows(DiaNoOperativoException.class,
                () -> horario.exigir(LocalDate.of(2026, 11, 22), "08:00-10:00", now));
        assertThrows(FechaFueraDelHorizonteException.class,
                () -> horario.exigir(LocalDate.of(2026, 12, 17), "08:00-10:00", now));
        assertThrows(FranjaDesconocidaException.class,
                () -> horario.exigir(LocalDate.of(2026, 11, 17), "16:00-18:00", now));
        assertThrows(SinAnticipacionSuficienteException.class,
                () -> horario.exigir(LocalDate.of(2026, 11, 16), "08:00-10:00", now));
        assertDoesNotThrow(() -> horario.exigir(LocalDate.of(2026, 11, 17), "08:00-10:00", now));
    }

    @Test
    void anticipacionSeAplicaAunqueLaFranjaSeaAlDiaSiguiente() {
        Instant medianoche = Instant.parse("2026-11-17T04:00:00Z");
        HorarioDeRecogida especial = new HorarioDeRecogida(ZoneId.of("America/Bogota"),
                Set.of(DayOfWeek.TUESDAY), List.of("01:00-03:00"), Duration.ofHours(4), 30);
        assertThrows(SinAnticipacionSuficienteException.class,
                () -> especial.exigir(LocalDate.of(2026, 11, 17), "01:00-03:00", medianoche));
    }
}
