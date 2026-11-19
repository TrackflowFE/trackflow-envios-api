package com.trackflow.modules.chatbot.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class NormalizadorTest {

    @Test
    void quita_tildes_signos_y_mayusculas() {
        assertEquals("cuanto cuesta enviar", Normalizador.normalizar("¿Cuánto CUESTA enviar?!"));
        assertEquals("ano nino", Normalizador.normalizar("Año, niño"));
        assertEquals("", Normalizador.normalizar(null));
    }

    @Test
    void las_palabras_ignoran_las_vacias_y_las_repetidas() {
        assertEquals(List.of("rastrear", "paquete"),
                List.copyOf(Normalizador.palabras("Quiero rastrear mi paquete, mi paquete")));
        assertTrue(Normalizador.palabras("de la el").isEmpty());
    }
}
