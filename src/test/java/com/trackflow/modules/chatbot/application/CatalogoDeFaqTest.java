package com.trackflow.modules.chatbot.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trackflow.modules.chatbot.domain.PreguntaFrecuente;
import com.trackflow.modules.chatbot.domain.PreguntaFrecuente.Contenido;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogoDeFaqTest {

    private static final Instant AHORA = Instant.parse("2026-11-19T15:00:00Z");

    private final FaqEnMemoria repositorio = new FaqEnMemoria();
    private final CatalogoDeFaq catalogo = new CatalogoDeFaq(repositorio, 0.35);

    private void agregar(String intencion, String categoria, String pregunta, List<String> claves, int orden) {
        repositorio.save(PreguntaFrecuente.crear(intencion, new Contenido(categoria, pregunta, claves,
                "Respuesta de " + intencion + ".", orden), "t", AHORA));
    }

    private String intencionPara(String mensaje) {
        return catalogo.mejor(mensaje).map(c -> c.entrada().intencion()).orElse("ninguna");
    }

    @Test
    void elige_la_pregunta_con_mas_palabras_en_comun() {
        agregar("rastrear", "seguimiento", "¿Cómo rastreo mi envío?", List.of("rastrear", "rastreo", "guia"), 1);
        agregar("recoger", "recogida", "¿Cómo pido una recogida?", List.of("recoger", "recogida"), 2);
        catalogo.recargar();

        assertEquals("rastrear", intencionPara("como RASTREO mi envío?"));
        assertEquals("recoger", intencionPara("necesito una recogida"));
    }

    @Test
    void sin_coincidencia_suficiente_no_contesta() {
        agregar("rastrear", "seguimiento", "¿Cómo rastreo mi envío?", List.of("rastrear", "guia"), 1);
        catalogo.recargar();

        assertEquals("ninguna", intencionPara("quien gano el partido de ayer"));
        assertEquals("ninguna", intencionPara("???"));
        assertEquals("ninguna", intencionPara("de la el"));
    }

    @Test
    void una_palabra_clave_basta_cuando_es_todo_el_mensaje() {
        agregar("saludo", "general", "Hola, buenas", List.of("hola", "buenas"), 1);
        catalogo.recargar();

        assertEquals("saludo", intencionPara("Hola!"));
    }

    @Test
    void un_empate_lo_gana_el_menor_orden() {
        agregar("segunda", "otra", "Pregunta sobre paquetes", List.of("paquetes"), 5);
        agregar("primera", "otra", "Otra sobre paquetes", List.of("paquetes"), 2);
        catalogo.recargar();

        assertEquals("primera", intencionPara("paquetes"));
    }

    @Test
    void las_desactivadas_no_contestan_hasta_que_se_recarga() {
        agregar("rastrear", "seguimiento", "¿Cómo rastreo mi envío?", List.of("rastreo"), 1);
        catalogo.recargar();
        assertEquals("rastrear", intencionPara("rastreo"));

        repositorio.faqs.get(0).cambiarActivo(false, "t", AHORA);
        assertEquals("rastrear", intencionPara("rastreo"));
        catalogo.recargar();
        assertEquals("ninguna", intencionPara("rastreo"));
    }

    @Test
    void las_sugerencias_salen_por_orden_sin_el_saludo_y_con_tope() {
        agregar("saludo", "general", "Hola, buenas", List.of("hola"), 0);
        for (int i = 1; i <= 8; i++) {
            agregar("pregunta_" + i, "otra", "Pregunta número " + i, List.of("palabra" + i), i);
        }
        catalogo.recargar();

        List<CatalogoDeFaq.Entrada> sugerencias = catalogo.sugerencias(6);

        assertEquals(6, sugerencias.size());
        assertEquals("pregunta_1", sugerencias.get(0).intencion());
        assertTrue(sugerencias.stream().noneMatch(e -> e.intencion().equals("saludo")));
    }

    @Test
    void rechaza_umbrales_invalidos() {
        assertThrows(IllegalArgumentException.class, () -> new CatalogoDeFaq(repositorio, 0));
        assertThrows(IllegalArgumentException.class, () -> new CatalogoDeFaq(repositorio, 1.5));
    }
}
