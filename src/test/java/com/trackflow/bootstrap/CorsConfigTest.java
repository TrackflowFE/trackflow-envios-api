package com.trackflow.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

class CorsConfigTest {

    /** Expone las configuraciones registradas, que CorsRegistry solo deja ver a sus subclases. */
    private static final class Registro extends CorsRegistry {
        Map<String, CorsConfiguration> configuraciones() {
            return getCorsConfigurations();
        }
    }

    @Test
    void soloLosOrigenesConfiguradosPuedenLlamarALaApi() {
        Registro registro = new Registro();

        new CorsConfig(new String[] { "http://localhost:5173", "https://trackflow.example" })
                .corsConfigurer().addCorsMappings(registro);

        CorsConfiguration api = registro.configuraciones().get("/api/**");
        assertEquals(List.of("http://localhost:5173", "https://trackflow.example"), api.getAllowedOrigins());
        assertEquals(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"), api.getAllowedMethods());
        assertFalse(api.getAllowedOrigins().contains("*"));
        assertEquals("https://trackflow.example", api.checkOrigin("https://trackflow.example"));
        assertNull(api.checkOrigin("https://atacante.example"));
    }
}
