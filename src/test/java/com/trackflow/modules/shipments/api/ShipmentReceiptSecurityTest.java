package com.trackflow.modules.shipments.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HU-14: el comprobante lleva nombres de personas, así que no se sirve sin sesión.
 *
 * Se activa la protección con una clave de operador para que la cadena de seguridad
 * real evalúe la ruta; sin ella, SecurityConfig deja todo abierto a propósito.
 */
@SpringBootTest(properties = {
        "trackflow.security.operador-clave=clave-de-prueba",
        "trackflow.security.jwt-secret=secreto-de-prueba-con-mas-de-32-caracteres"
})
@AutoConfigureMockMvc
class ShipmentReceiptSecurityTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void sinTokenElComprobanteResponde401() throws Exception {
        mvc.perform(get("/api/shipments/TF0123456789AB/receipt"))
                .andExpect(status().isUnauthorized());
    }
}
