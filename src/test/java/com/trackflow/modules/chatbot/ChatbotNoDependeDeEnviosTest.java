package com.trackflow.modules.chatbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * La garantía de privacidad de HU-21: el chatbot no puede consultar envíos ni cuentas
 * porque ni siquiera los conoce. Si alguien agrega una dependencia hacia ellos, esta
 * prueba falla.
 */
class ChatbotNoDependeDeEnviosTest {

    private static final Path CODIGO = Path.of("src/main/java/com/trackflow/modules/chatbot");
    private static final List<String> PROHIBIDOS = List.of("com.trackflow.modules.shipments",
            "com.trackflow.modules.pickups", "com.trackflow.modules.pqr", "com.trackflow.modules.reports",
            "com.trackflow.modules.logistics", "javax.sql", "java.sql", "JdbcTemplate", "EntityManager");

    @Test
    void ningun_archivo_del_chatbot_importa_envios_cuentas_ni_acceso_directo_a_la_base() throws IOException {
        try (Stream<Path> archivos = Files.walk(CODIGO)) {
            List<Path> java = archivos.filter(p -> p.toString().endsWith(".java")).toList();
            assertTrue(java.size() > 10, "se encontró el código del chatbot");
            for (Path archivo : java) {
                String texto = Files.readString(archivo, StandardCharsets.UTF_8);
                for (String prohibido : PROHIBIDOS) {
                    assertFalse(texto.contains(prohibido), archivo.getFileName() + " usa " + prohibido);
                }
            }
        }
    }

    @Test
    void la_unica_tabla_que_toca_es_la_de_preguntas() throws IOException {
        String migracion = Files.readString(Path.of("src/main/resources/db/migration/V32__chatbot_faq.sql"),
                StandardCharsets.UTF_8);
        assertEquals(0, migracion.split("references", -1).length - 1, "sin llaves foráneas hacia otras tablas");
    }
}
