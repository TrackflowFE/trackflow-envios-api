package com.trackflow.modules.pickups.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.DefaultCorsProcessor;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class LimiteDeIntentosFilter extends OncePerRequestFilter {
    private static final String BASE = "/api/pickups/self-service";
    private static final int MAX_KEYS = 10000;
    private final Map<String, Ventana> ips = new HashMap<>();
    private final Map<String, Ventana> fallos = new HashMap<>();
    private final int limiteIp;
    private final int limiteRecogida;
    private final long ventanaMillis;
    private final Clock clock;
    private final CorsConfigurationSource cors;

    private record Ventana(long hasta, int cantidad) { }

    public LimiteDeIntentosFilter(@Value("${trackflow.pickups.limite.ip:100}") int limiteIp,
            @Value("${trackflow.pickups.limite.recogida:10}") int limiteRecogida,
            @Value("${trackflow.pickups.limite.ventana-segundos:900}") long segundos, Clock clock,
            CorsConfigurationSource cors) {
        if (limiteIp < 1 || limiteRecogida < 1 || segundos < 1 || segundos > 86400) {
            throw new IllegalArgumentException("Los límites de autoservicio deben ser positivos y la ventana de hasta 86400 segundos");
        }
        this.limiteIp = limiteIp;
        this.limiteRecogida = limiteRecogida;
        this.ventanaMillis = segundos * 1000;
        this.clock = clock;
        this.cors = cors;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return "OPTIONS".equals(request.getMethod()) || !(path.equals(BASE) || path.startsWith(BASE + "/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        String path = request.getServletPath();
        String resto = path.length() > BASE.length() ? path.substring(BASE.length() + 1) : "";
        String numero = resto.split("/", 2)[0];
        int parametros = numero.indexOf(';');
        if (parametros >= 0) {
            numero = numero.substring(0, parametros);
        }
        Long reserva = null;
        boolean ipAdmitida = registrar(ips, request.getRemoteAddr(), limiteIp) != null;
        if (ipAdmitida && !numero.isEmpty()) {
            reserva = registrar(fallos, numero, limiteRecogida);
        }
        if (!ipAdmitida || (!numero.isEmpty() && reserva == null)) {
            CorsConfiguration configuracion = cors.getCorsConfiguration(request);
            if (configuracion != null && !new DefaultCorsProcessor().processRequest(configuracion, request, response)) {
                return;
            }
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(Math.max(1, ventanaMillis / 1000)));
            response.setContentType("application/problem+json");
            response.getWriter().write("{\"title\":\"Demasiados intentos\",\"status\":429}");
            return;
        }
        HttpServletResponseWrapper captura = new HttpServletResponseWrapper(response);
        try {
            chain.doFilter(request, captura);
        } finally {
            if (!numero.isEmpty() && captura.getStatus() != 404) {
                liberar(fallos, numero, reserva);
            }
        }
    }

    private synchronized void liberar(Map<String, Ventana> mapa, String clave, Long hasta) {
        Ventana ventana = mapa.get(clave);
        if (ventana != null && hasta != null && ventana.hasta() == hasta) {
            if (ventana.cantidad() <= 1) {
                mapa.remove(clave);
            } else {
                mapa.put(clave, new Ventana(ventana.hasta(), ventana.cantidad() - 1));
            }
        }
    }

    private synchronized Long registrar(Map<String, Ventana> mapa, String clave, int limite) {
        long ahora = clock.millis();
        Ventana ventana = mapa.get(clave);
        if (ventana == null || ventana.hasta() <= ahora) {
            mapa.entrySet().removeIf(e -> e.getValue().hasta() <= ahora);
            if (mapa.size() >= MAX_KEYS && !mapa.containsKey(clave)) {
                return null;
            }
            mapa.put(clave, new Ventana(ahora + ventanaMillis, 1));
            return ahora + ventanaMillis;
        }
        if (ventana.cantidad() >= limite) {
            return null;
        }
        mapa.put(clave, new Ventana(ventana.hasta(), ventana.cantidad() + 1));
        return ventana.hasta();
    }
}
