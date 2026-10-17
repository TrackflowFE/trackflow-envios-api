package com.trackflow.bootstrap.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

    @Test
    void declaraElEsquemaBearerJwtYLoExigeParaQueSwaggerAdjunteElToken() {
        OpenAPI api = new OpenApiConfig().trackflowOpenApi();

        assertEquals("TrackFlow API", api.getInfo().getTitle());
        assertEquals("v1", api.getInfo().getVersion());
        SecurityScheme esquema = api.getComponents().getSecuritySchemes().get("TokenDeOperador");
        assertEquals(SecurityScheme.Type.HTTP, esquema.getType());
        assertEquals("bearer", esquema.getScheme());
        assertEquals("JWT", esquema.getBearerFormat());
        assertTrue(api.getSecurity().get(0).containsKey("TokenDeOperador"));
    }
}
