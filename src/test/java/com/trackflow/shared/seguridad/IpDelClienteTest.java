package com.trackflow.shared.seguridad;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.http.HttpServletRequestWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.ForwardedHeaderFilter;

class IpDelClienteTest {
    private MockHttpServletRequest request(String peer, String xff) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(peer);
        if (xff != null) request.addHeader("X-Forwarded-For", xff);
        return request;
    }

    @Test
    void soloConfiaEnPeerConfiguradoYUsaLaDerecha() {
        IpDelCliente ips = new IpDelCliente(1, "192.0.2.10");
        assertEquals("198.51.100.2", ips.obtener(request("192.0.2.10", "203.0.113.1, 198.51.100.2")));
        assertEquals("192.0.2.11", ips.obtener(request("192.0.2.11", "198.51.100.2")));
        assertEquals("192.0.2.10", ips.obtener(request("192.0.2.10", null)));
        assertEquals("192.0.2.10", ips.obtener(request("192.0.2.10", "no-es-ip")));
        assertEquals("192.0.2.10", new IpDelCliente(0, "").obtener(request("192.0.2.10", "198.51.100.2")));
        assertThrows(IllegalArgumentException.class, () -> new IpDelCliente(1, ""));
    }

    @Test
    void exigeConfianzaEnCadaProxyDeLaCadena() {
        IpDelCliente ips = new IpDelCliente(2, "192.0.2.10,192.0.2.11");
        assertEquals("198.51.100.2", ips.obtener(request("192.0.2.10", "198.51.100.2, 192.0.2.11")));
        assertEquals("192.0.2.10", ips.obtener(request("192.0.2.10", "198.51.100.2, 192.0.2.12")));
        assertEquals("192.0.2.10", ips.obtener(request("192.0.2.10", "198.51.100.2")));
    }

    @Test
    void recuperaPeticionOriginalAunqueSpringLaEnvuelva() throws Exception {
        MockHttpServletRequest request = request("192.0.2.10", "203.0.113.1, 198.51.100.2");
        new ForwardedHeaderFilter().doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            var envuelta = new HttpServletRequestWrapper((jakarta.servlet.http.HttpServletRequest) req);
            assertEquals("198.51.100.2", new IpDelCliente(1, "192.0.2.10").obtener(envuelta));
            assertEquals("192.0.2.10", new IpDelCliente(0, "").obtener(envuelta));
        });
    }

    @Test
    void direccionRemotaNoLiteralNoRompeLaPeticion() {
        assertEquals("desconocida", new IpDelCliente(0, "").obtener(request("fe80::1%eth0", null)));
    }

    @Test
    void aceptaRangosCidrIpv4YExactos() {
        IpDelCliente ips = new IpDelCliente(1, "10.0.0.0/8,192.0.2.10");
        // Dentro del rango: se usa la IP de la cabecera.
        assertEquals("198.51.100.2", ips.obtener(request("10.20.30.40", "198.51.100.2")));
        assertEquals("198.51.100.3", ips.obtener(request("192.0.2.10", "198.51.100.3")));
        // Fuera del rango: la cabecera se ignora.
        assertEquals("192.0.2.11", ips.obtener(request("192.0.2.11", "198.51.100.2")));
        assertEquals("11.0.0.1", ips.obtener(request("11.0.0.1", "198.51.100.2")));
    }

    @Test
    void aceptaRangosCidrIpv6YNoMezclaFamilias() {
        IpDelCliente ips = new IpDelCliente(1, "fd00::/8,2001:db8::/32");
        // Peer dentro del rango: se usa la IP de la cabecera.
        assertEquals("2001:db8:1:2:3:4:5:6",
                ips.obtener(request("fd00::10", "2001:db8:1:2:3:4:5:6")));
        // Peer fuera del rango: la cabecera se ignora.
        assertEquals("2001:db9:0:0:0:0:0:20", ips.obtener(request("2001:db9::20", "2001:db8::1")));
        // Una IPv4 nunca entra en un rango IPv6.
        assertEquals("192.0.2.10", ips.obtener(request("192.0.2.10", "2001:db8::1")));
    }

    @Test
    void laCadenaRevisaTodosLosSaltosDentroDelRango() {
        IpDelCliente ips = new IpDelCliente(2, "10.0.0.0/8");
        assertEquals("203.0.113.9", ips.obtener(request("10.1.2.3", "203.0.113.9, 10.9.9.9")));
        // El último salto no está en el rango: la cadena no es confiable.
        assertEquals("10.1.2.3", ips.obtener(request("10.1.2.3", "203.0.113.9, 192.0.2.5")));
    }

    @Test
    void rechazaPrefijosInvalidosAlArrancar() {
        for (String rango : new String[] { "10.0.0.0/33", "fd00::/129", "10.0.0.0/abc",
                "10.0.0.0/", "10.0.0.0/8/8", "10.0.0.0/-1" }) {
            assertThrows(IllegalArgumentException.class, () -> new IpDelCliente(1, rango), rango);
        }
    }

    @Test
    void conSaltosEnCeroIgnoraLaCabeceraAunqueLlegue() {
        assertEquals("192.0.2.10", new IpDelCliente(0, "10.0.0.0/8")
                .obtener(request("192.0.2.10", "198.51.100.2")));
    }
}
