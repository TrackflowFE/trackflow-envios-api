package com.trackflow.modules.pqr.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trackflow.modules.pqr.domain.EstadoPqr;
import com.trackflow.modules.pqr.domain.Pqr;
import com.trackflow.modules.pqr.domain.TipoPqr;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RadicarPqrTest {

    private final PqrEnMemoria repositorio = new PqrEnMemoria();
    private final PublicadorGrabador publicador = new PublicadorGrabador();
    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-19T15:00:00Z"), ZoneOffset.UTC);
    private final RadicarPqr radicar = new RadicarPqr(repositorio, () -> "PQR-2026-00042", publicador, reloj);

    private static RadicarPqr.Solicitud solicitud(String descripcion, String telefono, String guia, boolean acepta) {
        return new RadicarPqr.Solicitud(TipoPqr.RECLAMO, descripcion, "Ana Pérez", "ANA@correo.com", telefono, guia,
                acepta);
    }

    @Test
    void radica_guarda_deja_bitacora_y_avisa() {
        Pqr pqr = radicar.radicar(solicitud("El paquete llegó golpeado y abierto.", "3001234567", "TFABC123DEF456",
                true));

        assertThat(pqr.radicado()).isEqualTo("PQR-2026-00042");
        assertThat(pqr.estado()).isEqualTo(EstadoPqr.RECIBIDA);
        assertThat(repositorio.pqrs).containsExactly(pqr);
        assertThat(repositorio.eventos).hasSize(1);
        assertThat(publicador.radicadas).singleElement().satisfies(e -> {
            assertThat(e.radicado()).isEqualTo("PQR-2026-00042");
            assertThat(e.email()).isEqualTo("ana@correo.com");
            assertThat(e.occurredAt()).isEqualTo(reloj.instant());
        });
    }

    @Test
    void guarda_el_texto_malicioso_sin_alterarlo() {
        String codigo = "<script>alert('x')</script> y <img src=x onerror=alert(1)> \"> en la descripción";

        Pqr pqr = radicar.radicar(solicitud(codigo, null, null, true));

        assertThat(pqr.descripcion()).isEqualTo(codigo);
    }

    @Test
    void quita_caracteres_de_control_pero_conserva_saltos_de_linea() {
        Pqr pqr = radicar.radicar(solicitud("  linea uno\u0000\nlinea dos\u0007 con texto suficiente  ", null, null,
                true));

        assertThat(pqr.descripcion()).isEqualTo("linea uno\nlinea dos con texto suficiente");
    }

    @Test
    void telefono_y_guia_en_blanco_quedan_nulos() {
        Pqr pqr = radicar.radicar(solicitud("Descripción suficientemente larga.", "  ", " ", true));

        assertThat(pqr.contactoTelefono()).isNull();
        assertThat(pqr.numeroSeguimiento()).isNull();
    }

    @Test
    void sin_aceptar_el_tratamiento_de_datos_no_guarda_ni_avisa() {
        assertThatThrownBy(() -> radicar.radicar(solicitud("Descripción suficientemente larga.", null, null, false)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(repositorio.pqrs).isEmpty();
        assertThat(publicador.radicadas).isEmpty();
    }

    @Test
    void textoPlano_deja_nulo_el_nulo() {
        assertThat(TextoPlano.limpiar(null)).isNull();
    }
}
