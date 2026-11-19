package com.trackflow.modules.chatbot.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Un reloj que solo avanza cuando la prueba lo pide. */
class RelojManual extends Clock {

    private Instant ahora = Instant.parse("2026-11-19T15:00:00Z");

    void avanzar(Duration duracion) {
        ahora = ahora.plus(duracion);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return ahora;
    }
}
