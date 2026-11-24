package com.trackflow.bootstrap.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trackflow.shared.seguridad.IpDelCliente;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * El bloqueo de cuenta de HU-09 protege a un usuario; este filtro protege al servicio
 * frente a quien va rotando nombres y nunca acumula fallos en ninguno.
 */
class LimiteDeLoginFilterTest {

    private static final Instant AHORA = Instant.parse("2026-11-24T12:00:00Z");

    private MockHttpServletRequest peticion(String ruta, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", ruta);
        request.setServletPath(ruta);
        request.setRemoteAddr(ip);
        return request;
    }

    private int pasar(LimiteDeLoginFilter filtro, String ruta, String ip) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filtro.doFilter(peticion(ruta, ip), response, (req, res) -> { /* la API respondería aquí */ });
        return response.getStatus();
    }

    @Test
    void cortaElLoginAlPasarseDelLimiteAunqueCadaIntentoUseOtroUsuario() throws Exception {
        LimiteDeLoginFilter filtro = new LimiteDeLoginFilter(3, 60,
                Clock.fixed(AHORA, ZoneOffset.UTC), new IpDelCliente(0, ""));

        for (int i = 0; i < 3; i++) {
            assertEquals(200, pasar(filtro, "/api/auth/login", "203.0.113.7"));
        }
        assertEquals(429, pasar(filtro, "/api/auth/login", "203.0.113.7"));
    }

    @Test
    void el429LlevaRetryAfterYNoSeGuardaEnCache() throws Exception {
        LimiteDeLoginFilter filtro = new LimiteDeLoginFilter(1, 45,
                Clock.fixed(AHORA, ZoneOffset.UTC), new IpDelCliente(0, ""));
        pasar(filtro, "/api/auth/login", "203.0.113.7");

        MockHttpServletResponse response = new MockHttpServletResponse();
        filtro.doFilter(peticion("/api/auth/login", "203.0.113.7"), response, (req, res) -> { /* no llega */ });

        assertEquals(429, response.getStatus());
        assertEquals("45", response.getHeader("Retry-After"));
        assertEquals("no-store", response.getHeader("Cache-Control"));
        assertEquals("application/problem+json", response.getContentType());
        // El cuerpo no dice si el usuario existe ni si la clave era correcta.
        assertTrue(response.getContentAsString().contains("Demasiadas peticiones"));
    }

    @Test
    void laCuotaEsPorIpAsiQueOtroClienteNoPagaPorElAtacante() throws Exception {
        LimiteDeLoginFilter filtro = new LimiteDeLoginFilter(1, 60,
                Clock.fixed(AHORA, ZoneOffset.UTC), new IpDelCliente(0, ""));
        pasar(filtro, "/api/auth/login", "203.0.113.7");

        assertEquals(429, pasar(filtro, "/api/auth/login", "203.0.113.7"));
        assertEquals(200, pasar(filtro, "/api/auth/login", "198.51.100.4"));
    }

    @Test
    void laVentanaSeLiberaCuandoPasaElTiempo() throws Exception {
        MutableClock reloj = new MutableClock(AHORA);
        LimiteDeLoginFilter filtro = new LimiteDeLoginFilter(1, 60, reloj, new IpDelCliente(0, ""));

        pasar(filtro, "/api/auth/login", "203.0.113.7");
        assertEquals(429, pasar(filtro, "/api/auth/login", "203.0.113.7"));

        reloj.avanzar(Duration.ofSeconds(61));
        assertEquals(200, pasar(filtro, "/api/auth/login", "203.0.113.7"));
    }

    @Test
    void tambienLimitaLaRecuperacionDeClavePorqueCadaPeticionMandaUnCorreo() throws Exception {
        LimiteDeLoginFilter filtro = new LimiteDeLoginFilter(2, 60,
                Clock.fixed(AHORA, ZoneOffset.UTC), new IpDelCliente(0, ""));

        assertEquals(200, pasar(filtro, "/api/auth/clave/recuperar", "203.0.113.7"));
        assertEquals(200, pasar(filtro, "/api/auth/clave/recuperar", "203.0.113.7"));
        assertEquals(429, pasar(filtro, "/api/auth/clave/recuperar", "203.0.113.7"));
    }

    @Test
    void noSeMeteConLoQueNoEsAutenticacion() {
        LimiteDeLoginFilter filtro = new LimiteDeLoginFilter(1, 60,
                Clock.fixed(AHORA, ZoneOffset.UTC), new IpDelCliente(0, ""));

        assertTrue(filtro.shouldNotFilter(peticion("/api/shipments", "203.0.113.7")));
        // El cambio de clave ya exige token: lo protege la cadena de seguridad, no este filtro.
        assertTrue(filtro.shouldNotFilter(peticion("/api/auth/clave/cambiar", "203.0.113.7")));
    }

    @Test
    void elPreflightNoConsumeCuotaDelUsuario() {
        LimiteDeLoginFilter filtro = new LimiteDeLoginFilter(1, 60,
                Clock.fixed(AHORA, ZoneOffset.UTC), new IpDelCliente(0, ""));

        MockHttpServletRequest preflight = peticion("/api/auth/login", "203.0.113.7");
        preflight.setMethod("OPTIONS");
        assertTrue(filtro.shouldNotFilter(preflight));
    }

    @Test
    void rechazaUnaConfiguracionQueDejariaElLimiteSinSentido() {
        Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
        IpDelCliente ip = new IpDelCliente(0, "");

        assertThrows(IllegalArgumentException.class, () -> new LimiteDeLoginFilter(0, 60, reloj, ip));
        assertThrows(IllegalArgumentException.class, () -> new LimiteDeLoginFilter(10, 0, reloj, ip));
        assertThrows(IllegalArgumentException.class, () -> new LimiteDeLoginFilter(10, 86401, reloj, ip));
    }

    /** Reloj que se puede adelantar, para comprobar que la ventana caduca. */
    private static final class MutableClock extends Clock {
        private Instant instante;

        private MutableClock(Instant instante) {
            this.instante = instante;
        }

        void avanzar(Duration cuanto) {
            instante = instante.plus(cuanto);
        }

        @Override
        public Instant instant() {
            return instante;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zona) {
            return this;
        }
    }
}
