package com.trackflow.shared.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class IpDelClienteRangosTest {

    private MockHttpServletRequest request(String peer, String xff) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(peer);
        request.addHeader("X-Forwarded-For", xff);
        return request;
    }

    @Test
    void unRangoConPrefijoQueNoCaeEnByteCompletoReconoceAlProxy() {
        IpDelCliente ips = new IpDelCliente(1, "10.16.0.0/12");

        assertEquals("198.51.100.2", ips.obtener(request("10.31.255.1", "198.51.100.2")));
    }

    @Test
    void unaIpFueraDelRangoNoSeTrataComoProxy() {
        IpDelCliente ips = new IpDelCliente(1, "10.16.0.0/12");

        assertEquals("10.32.0.1", ips.obtener(request("10.32.0.1", "198.51.100.2")));
    }

    @Test
    void losSaltosFueraDeRangoSeRechazan() {
        assertThrows(IllegalArgumentException.class, () -> new IpDelCliente(-1, ""));
        assertThrows(IllegalArgumentException.class, () -> new IpDelCliente(17, "10.0.0.1"));
    }
}
