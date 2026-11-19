package com.trackflow.modules.chatbot.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trackflow.modules.chatbot.domain.PreguntaFrecuente.Contenido;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class PreguntaFrecuenteTest {

    private static final Instant AHORA = Instant.parse("2026-11-19T15:00:00Z");

    private static Contenido contenido() {
        return new Contenido("Seguimiento", "¿Cómo rastreo mi envío?", List.of("Rastreo", "GUÍA", "rastreo"),
                "Escribe tu guía en la página de rastreo.", 3);
    }

    @Test
    void crea_activa_con_palabras_normalizadas_y_sin_repetir() {
        PreguntaFrecuente faq = PreguntaFrecuente.crear("como_rastrear", contenido(), "admin", AHORA);

        assertTrue(faq.activo());
        assertEquals("seguimiento", faq.categoria());
        assertEquals(List.of("rastreo", "guia"), faq.palabrasClave());
        assertEquals("admin", faq.updatedBy());
        assertEquals(AHORA, faq.updatedAt());
    }

    @Test
    void rechaza_intenciones_mal_formadas() {
        for (String mala : new String[] {null, "", "ab", "Como", "1como", "con espacio", "con-guion"}) {
            assertThrows(IllegalArgumentException.class, () -> PreguntaFrecuente.crear(mala, contenido(), "a", AHORA));
        }
    }

    @Test
    void valida_los_textos_y_las_palabras_clave() {
        assertThrows(IllegalArgumentException.class,
                () -> new Contenido("x", "pregunta valida", List.of("a"), "respuesta valida", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Contenido("cat", "corta", List.of("a"), "respuesta valida", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Contenido("cat", "pregunta valida", List.of("a"), "corta", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Contenido("cat", "pregunta valida", List.of(), "respuesta valida", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Contenido("cat", "pregunta valida", List.of("???"), "respuesta valida", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Contenido("cat", "pregunta valida", List.of("a".repeat(31)), "respuesta valida", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Contenido("cat", "pregunta valida", List.of("hola"), "respuesta valida", 1000));
        List<String> muchas = IntStream.range(0, 16).mapToObj(i -> "palabra" + i).toList();
        assertThrows(IllegalArgumentException.class,
                () -> new Contenido("cat", "pregunta valida", muchas, "respuesta valida", 0));
    }

    @Test
    void rechaza_caracteres_de_control_pero_deja_los_saltos_de_linea() {
        assertThrows(IllegalArgumentException.class,
                () -> new Contenido("cat", "pregunta valida", List.of("hola"), "respuesta\u0000valida", 0));
        assertEquals("primera linea\nsegunda linea",
                new Contenido("cat", "pregunta valida", List.of("hola"), "primera linea\nsegunda linea", 0)
                        .respuesta());
    }

    @Test
    void edita_sin_cambiar_la_intencion_y_desactiva_sin_borrar() {
        PreguntaFrecuente faq = PreguntaFrecuente.crear("como_rastrear", contenido(), "admin", AHORA);
        Instant despues = AHORA.plusSeconds(60);

        faq.editar(new Contenido("pqr", "Otra pregunta distinta", List.of("otra"), "Otra respuesta distinta.", 7),
                "otro", despues);
        faq.cambiarActivo(false, "otro", despues);

        assertEquals("como_rastrear", faq.intencion());
        assertEquals("Otra respuesta distinta.", faq.respuesta());
        assertEquals(7, faq.orden());
        assertFalse(faq.activo());
        assertEquals("otro", faq.updatedBy());
    }
}
