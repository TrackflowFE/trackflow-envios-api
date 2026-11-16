package com.trackflow.modules.pickups.api;

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
}
