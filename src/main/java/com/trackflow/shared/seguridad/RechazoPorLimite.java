package com.trackflow.shared.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.DefaultCorsProcessor;

/**
 * La respuesta 429 de los filtros de límite.
 *
 * Aplica CORS antes de escribir: si no, el navegador oculta el 429 detrás de un error
 * de CORS y el frontend no puede decirle al usuario que espere.
 */
public final class RechazoPorLimite {

    private RechazoPorLimite() {
    }

    public static void responder(HttpServletRequest request, HttpServletResponse response,
            CorsConfigurationSource cors, long segundos, String titulo) throws IOException {
        CorsConfiguration configuracion = cors.getCorsConfiguration(request);
        if (configuracion != null && !new DefaultCorsProcessor().processRequest(configuracion, request, response)) {
            return;
        }
        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(Math.max(1, segundos)));
        response.setContentType("application/problem+json");
        response.getWriter().write("{\"title\":\"" + titulo + "\",\"status\":429}");
    }
}
