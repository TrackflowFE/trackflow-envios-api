package com.trackflow.modules.pickups.api;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;

class LimiteDeIntentosFilterTest {
    @Test
    void limitaFallosPorNumeroYAplicaCorsAl429() throws Exception {
        CorsConfiguration cors = new CorsConfiguration();
        cors.addAllowedOrigin("https://cliente.example");
        cors.addAllowedMethod("GET");
        LimiteDeIntentosFilter filtro = new LimiteDeIntentosFilter(10, 2, 60,
                Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC), request -> cors);
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/pickups/self-service/RC0123456789AB");
            request.setServletPath("/api/pickups/self-service/RC0123456789AB");
            request.addHeader("Origin", "https://cliente.example");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filtro.doFilter(request, response, (req, res) -> ((jakarta.servlet.http.HttpServletResponse) res).setStatus(404));
            assertEquals(i < 2 ? 404 : 429, response.getStatus());
            assertEquals("no-store", response.getHeader("Cache-Control"));
            if (i == 2) {
                assertEquals("https://cliente.example", response.getHeader("Access-Control-Allow-Origin"));
                assertEquals("60", response.getHeader("Retry-After"));
            }
        }
    }

    private MockHttpServletResponse solicitar(LimiteDeIntentosFilter filtro, String ip, String sufijo, int status)
            throws Exception {
        String path = "/api/pickups/self-service/" + sufijo;
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filtro.doFilter(request, response, (req, res) -> ((jakarta.servlet.http.HttpServletResponse) res).setStatus(status));
        return response;
    }

    @Test
    void limitaIpIndependientementeDelNumeroYCuentaExitos() throws Exception {
        LimiteDeIntentosFilter filtro = new LimiteDeIntentosFilter(2, 10, 60, Clock.systemUTC(), request -> null);
        assertEquals(200, solicitar(filtro, "192.0.2.1", "RC0123456789AB", 200).getStatus());
        assertEquals(200, solicitar(filtro, "192.0.2.1", "RC0123456789AC", 200).getStatus());
        MockHttpServletResponse bloqueada = solicitar(filtro, "192.0.2.1", "RC0123456789AD", 200);
        assertEquals(429, bloqueada.getStatus());
        assertEquals("60", bloqueada.getHeader("Retry-After"));
        assertEquals(200, solicitar(filtro, "192.0.2.2", "RC0123456789AD", 200).getStatus());
    }

    @Test
    void limitaNumeroEntreIpsNormalizaParametrosYLiberaExitos() throws Exception {
        LimiteDeIntentosFilter filtro = new LimiteDeIntentosFilter(20, 2, 60, Clock.systemUTC(), request -> null);
        assertEquals(404, solicitar(filtro, "192.0.2.1", "RC0123456789AB;foo=bar", 404).getStatus());
        assertEquals(200, solicitar(filtro, "192.0.2.2", "RC0123456789AB/schedule", 200).getStatus());
        assertEquals(404, solicitar(filtro, "192.0.2.2", "RC0123456789AB/cancel", 404).getStatus());
        assertEquals(429, solicitar(filtro, "192.0.2.3", "RC0123456789AB", 200).getStatus());
        assertEquals(200, solicitar(filtro, "192.0.2.3", "RC0123456789AC", 200).getStatus());
    }

    @Test
    void reservaConcurrenteImpideSuperarLimiteYSeLiberaAlCompletar() throws Exception {
        LimiteDeIntentosFilter filtro = new LimiteDeIntentosFilter(20, 1, 60, Clock.systemUTC(), request -> null);
        CountDownLatch dentro = new CountDownLatch(1);
        CountDownLatch continuar = new CountDownLatch(1);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var primera = executor.submit(() -> {
                String path = "/api/pickups/self-service/RC0123456789AB";
                MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
                request.setServletPath(path);
                MockHttpServletResponse response = new MockHttpServletResponse();
                filtro.doFilter(request, response, (req, res) -> {
                    dentro.countDown();
                    try {
                        if (!continuar.await(5, TimeUnit.SECONDS)) throw new AssertionError("Tiempo de espera agotado");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    }
                    ((jakarta.servlet.http.HttpServletResponse) res).setStatus(200);
                });
                return response.getStatus();
            });
            try {
                assertTrue(dentro.await(5, TimeUnit.SECONDS));
                assertEquals(429, solicitar(filtro, "192.0.2.2", "RC0123456789AB", 200).getStatus());
            } finally {
                continuar.countDown();
            }
            assertEquals(200, primera.get(5, TimeUnit.SECONDS));
            assertEquals(200, solicitar(filtro, "192.0.2.2", "RC0123456789AB", 200).getStatus());
        }
    }
}
