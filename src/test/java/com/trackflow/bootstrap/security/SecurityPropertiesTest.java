package com.trackflow.bootstrap.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class SecurityPropertiesTest {

    private static final String SECRETO_LARGO = "0123456789abcdef0123456789abcdef";

    private static SecurityProperties con(String secreto, String claveOperador, String claveAdmin) {
        return new SecurityProperties(secreto, "operador", claveOperador, "admin", claveAdmin, 480, 30);
    }

    @Test
    void sinClavesLaProteccionQuedaDesactivada() {
        assertFalse(con("", "", "").proteccionActiva());
        assertFalse(con("", null, "  ").proteccionActiva());
    }

    @Test
    void conClaveDeOperadorOdeAdministradorLaProteccionSeActiva() {
        assertTrue(con("", "clave", "").proteccionActiva());
        assertTrue(con("", "", "clave").proteccionActiva());
    }

    @Test
    void elAdministradorSoloEstaConfiguradoSiTieneClave() {
        assertTrue(con("", "", "clave").adminConfigurado());
        assertFalse(con("", "clave", "").adminConfigurado());
        assertFalse(con("", "clave", null).adminConfigurado());
    }

    @Test
    void exponeLosUsuariosClavesYDuraciones() {
        SecurityProperties propiedades = new SecurityProperties(SECRETO_LARGO, "op", "c1", "ad", "c2", 60, 15);

        assertEquals("op", propiedades.operadorUsuario());
        assertEquals("c1", propiedades.operadorClave());
        assertEquals("ad", propiedades.adminUsuario());
        assertEquals("c2", propiedades.adminClave());
        assertEquals(Duration.ofMinutes(60), propiedades.duracionToken());
        assertEquals(Duration.ofMinutes(15), propiedades.inactividad());
    }

    @Test
    void unSecretoDeAlMenos32CaracteresSeUsaTalCual() {
        assertEquals(SECRETO_LARGO, con(SECRETO_LARGO, "clave", "").secretoEfectivo());
    }

    @Test
    void unSecretoCortoSeReemplazaPorUnoAleatorioDe32Bytes() {
        SecurityProperties propiedades = con("corto", "clave", "");

        String primero = propiedades.secretoEfectivo();
        String segundo = propiedades.secretoEfectivo();

        assertEquals(32, Base64.getDecoder().decode(primero).length);
        assertNotEquals("corto", primero);
        assertNotEquals(primero, segundo);
    }

    @Test
    void sinSecretoNiProteccionTambienSeGeneraUnoAleatorio() {
        String secreto = con(null, "", "").secretoEfectivo();

        assertEquals(32, Base64.getDecoder().decode(secreto).length);
    }
}
