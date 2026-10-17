package com.trackflow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Sin contexto de Spring: solo el reloj que comparten los casos de uso. */
class TrackflowApplicationTest {

    @Test
    void elRelojDeLaAplicacionEstaEnUtc() {
        Clock reloj = new TrackflowApplication().clock();

        assertEquals(ZoneOffset.UTC, reloj.getZone());
    }
}
