package com.trackflow.modules.chatbot.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trackflow.modules.chatbot.domain.PreguntaFrecuente;
import com.trackflow.modules.chatbot.domain.PreguntaFrecuente.Contenido;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Prueba el contenido que realmente se siembra en V33 con preguntas de personas: si
 * alguien cambia la migración o el buscador y el chatbot deja de entender lo básico,
 * esta prueba lo avisa.
 */
class ContenidoInicialTest {

    private static final Path MIGRACION = Path.of("src/main/resources/db/migration/V33__chatbot_faq_inicial.sql");
    private static final Pattern FILA = Pattern.compile(
            "\\('([a-z_]+)', '([a-z]+)', '([^']+)', '([^']+)', '([^']+)', (\\d+)\\)");

    private final FaqEnMemoria repositorio = new FaqEnMemoria();
    private final CatalogoDeFaq catalogo = new CatalogoDeFaq(repositorio, 0.35);

    @BeforeEach
    void cargar() throws IOException {
        Matcher fila = FILA.matcher(Files.readString(MIGRACION, StandardCharsets.UTF_8));
        while (fila.find()) {
            repositorio.save(PreguntaFrecuente.crear(fila.group(1), new Contenido(fila.group(2), fila.group(3),
                    Arrays.asList(fila.group(4).split(",")), fila.group(5), Integer.parseInt(fila.group(6))), "t",
                    Instant.parse("2026-11-19T15:00:00Z")));
        }
        catalogo.recargar();
    }

    private String intencionPara(String mensaje) {
        return catalogo.mejor(mensaje).map(c -> c.entrada().intencion()).orElse("ninguna");
    }

    @Test
    void la_migracion_se_lee_completa() {
        assertEquals(14, repositorio.faqs.size());
    }

    @Test
    void entiende_las_preguntas_basicas() {
        assertEquals("saludo", intencionPara("Hola"));
        assertEquals("como_rastrear", intencionPara("¿Cómo rastreo mi paquete?"));
        assertEquals("estados_envio", intencionPara("¿Qué significa en tránsito y en reparto?"));
        assertEquals("pedir_recogida", intencionPara("quiero pedir que recojan mi paquete"));
        assertEquals("cancelar_recogida", intencionPara("¿puedo cancelar la recogida?"));
        assertEquals("radicar_pqr", intencionPara("cómo radico una queja"));
        assertEquals("consultar_pqr", intencionPara("como consulto mi pqr con el radicado"));
        assertEquals("privacidad", intencionPara("que datos personales se ven en el rastreo"));
        assertEquals("paquete_no_llega", intencionPara("mi paquete no llego y esta roto"));
    }

    @Test
    void lo_que_no_es_del_servicio_queda_fuera_de_tema() {
        assertEquals("ninguna", intencionPara("quien gano el mundial"));
        assertEquals("ninguna", intencionPara("dame una receta de arepas"));
    }

    @Test
    void las_sugerencias_son_las_preguntas_de_la_migracion() {
        assertEquals(6, catalogo.sugerencias(6).size());
        for (CatalogoDeFaq.Entrada sugerida : catalogo.sugerencias(6)) {
            assertEquals(sugerida.intencion(), intencionPara(sugerida.pregunta()), sugerida.pregunta());
        }
    }

    @Test
    void no_promete_lo_que_el_sistema_no_define() {
        for (PreguntaFrecuente faq : repositorio.faqs) {
            String texto = (faq.pregunta() + " " + faq.respuesta()).toLowerCase(Locale.ROOT);
            for (String prohibida : List.of("tarifa", "precio", "$", "pago", "factura", "peso máximo", "2 horas",
                    "plazo legal", "días hábiles")) {
                assertFalse(texto.contains(prohibida), faq.intencion() + " menciona " + prohibida);
            }
        }
    }

    @Test
    void la_guia_de_ejemplo_no_es_una_guia_real() {
        String ejemplo = repositorio.faqs.stream().filter(f -> f.intencion().equals("formato_guia")).findFirst()
                .orElseThrow().respuesta();
        assertTrue(ejemplo.contains("TFXXXXXXXXXXXX"));
    }
}
