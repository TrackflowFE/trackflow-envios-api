package com.trackflow.modules.pqr.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trackflow.shared.seguridad.IpDelCliente;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;

class LimiteDePqrFilterTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-19T15:00:00Z"), ZoneOffset.UTC);

    private final CorsConfiguration cors = corsDelCliente();
    private final LimiteDePqrFilter filtro = new LimiteDePqrFilter(2, 3, 60, RELOJ, request -> cors,
            new IpDelCliente(0, ""));

    private static CorsConfiguration corsDelCliente() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.addAllowedOrigin("https://cliente.example");
        cors.addAllowedMethod("POST");
        return cors;
    }

    private MockHttpServletResponse enviar(String metodo, String ruta, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, ruta);
        request.setServletPath(ruta);
        request.setRemoteAddr(ip);
        request.addHeader("Origin", "https://cliente.example");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filtro.doFilter(request, response, (req, res) -> ((HttpServletResponse) res).setStatus(201));
        return response;
    }

    @Test
    void limita_radicar_por_ip_y_deja_pasar_a_otra_ip() throws Exception {
        assertEquals(201, enviar("POST", "/api/pqr", "192.0.2.1").getStatus());
        assertEquals(201, enviar("POST", "/api/pqr", "192.0.2.1").getStatus());

        MockHttpServletResponse rechazada = enviar("POST", "/api/pqr", "192.0.2.1");

        assertEquals(429, rechazada.getStatus());
        assertEquals("60", rechazada.getHeader("Retry-After"));
        assertEquals("https://cliente.example", rechazada.getHeader("Access-Control-Allow-Origin"));
        assertTrue(rechazada.getContentAsString().contains("Demasiados intentos"));
        assertEquals(201, enviar("POST", "/api/pqr", "192.0.2.2").getStatus());
    }

    @Test
    void consultar_tiene_su_propio_tope_independiente_de_radicar() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertEquals(201, enviar("POST", "/api/pqr/consulta", "192.0.2.1").getStatus());
        }
        assertEquals(429, enviar("POST", "/api/pqr/consulta", "192.0.2.1").getStatus());
        assertEquals(201, enviar("POST", "/api/pqr", "192.0.2.1").getStatus());
    }

    @Test
    void no_toca_la_gestion_ni_otros_metodos_ni_otras_rutas() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertEquals(201, enviar("GET", "/api/pqr", "192.0.2.1").getStatus());
            assertEquals(201, enviar("POST", "/api/pqr/PQR-2026-00042/estado", "192.0.2.1").getStatus());
            assertEquals(201, enviar("POST", "/api/shipments", "192.0.2.1").getStatus());
        }
    }

    @Test
    void rechaza_limites_invalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> new LimiteDePqrFilter(0, 3, 60, RELOJ, request -> cors, new IpDelCliente(0, "")));
        assertThrows(IllegalArgumentException.class,
                () -> new LimiteDePqrFilter(2, 0, 60, RELOJ, request -> cors, new IpDelCliente(0, "")));
        assertThrows(IllegalArgumentException.class,
                () -> new LimiteDePqrFilter(2, 3, 90000, RELOJ, request -> cors, new IpDelCliente(0, "")));
    }
}
