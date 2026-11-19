package com.trackflow.modules.pqr.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RadicadoPorSecuenciaTest {

    @Test
    void completa_el_numero_a_cinco_digitos() {
        assertThat(RadicadoPorSecuencia.formatear(2026, 42)).isEqualTo("PQR-2026-00042");
    }

    @Test
    void no_recorta_cuando_el_numero_pasa_de_cinco_digitos() {
        assertThat(RadicadoPorSecuencia.formatear(2027, 123456)).isEqualTo("PQR-2027-123456");
    }
}
