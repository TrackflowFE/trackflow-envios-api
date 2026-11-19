package com.trackflow.shared.seguridad;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class VentanasDeIntentosTest {
    @Test
    void saturacionDesalojaLaMenosUsadaSinContarLiberacionesAntiguas() {
        VentanasDeIntentos ventanas = new VentanasDeIntentos(2, 60000, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        Long primera = ventanas.reservar("a", 1);
        assertNotNull(primera);
        assertNotNull(ventanas.reservar("b", 1));
        assertNotNull(ventanas.reservar("c", 1));
        assertNull(ventanas.reservar("b", 1));
        assertNotNull(ventanas.reservar("a", 1));
        ventanas.liberar("a", primera);
        assertNull(ventanas.reservar("a", 1));
    }

    @Test
    void liberacionAntiguaNoAfectaLaVentanaActual() {
        class Reloj extends Clock {
            long millis;
            public ZoneOffset getZone() { return ZoneOffset.UTC; }
            public Clock withZone(java.time.ZoneId zone) { return this; }
            public Instant instant() { return Instant.ofEpochMilli(millis); }
        }
        Reloj reloj = new Reloj();
        VentanasDeIntentos ventanas = new VentanasDeIntentos(1, 1000, reloj);
        Long antigua = ventanas.reservar("a", 1);
        reloj.millis = 1000;
        assertNotNull(ventanas.reservar("a", 1));
        ventanas.liberar("a", antigua);
        assertNull(ventanas.reservar("a", 1));
    }
}
