package com.trackflow.modules.shipments.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class TrackingNumberTest {

    @Test
    void aceptaElPrefijoTfSeguidoDeDoceCaracteres() {
        TrackingNumber numero = TrackingNumber.of("TF0123456789AB");

        assertEquals("TF0123456789AB", numero.value());
        assertEquals("TF0123456789AB", numero.toString());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", "TF123", "XX0123456789AB", "TF0123456789ab", "TF0123456789ABC", "tf0123456789AB" })
    void rechazaLosValoresQueNoTienenElFormato(String valor) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> TrackingNumber.of(valor));

        assertEquals("Número de seguimiento inválido: " + valor, e.getMessage());
    }

    @Test
    void dosNumerosConElMismoValorSonIguales() {
        TrackingNumber a = TrackingNumber.of("TF0123456789AB");
        TrackingNumber b = TrackingNumber.of("TF0123456789AB");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void numerosConValoresDistintosNoSonIguales() {
        TrackingNumber a = TrackingNumber.of("TF0123456789AB");

        assertNotEquals(a, TrackingNumber.of("TF0123456789AC"));
        assertNotEquals(a, "TF0123456789AB");
        assertNotEquals(null, a);
    }
}
