package com.trackflow.modules.pqr.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.trackflow.bootstrap.security.EnvioDeCorreo;
import com.trackflow.modules.pqr.application.PqrEstadoCambiado;
import com.trackflow.modules.pqr.application.PqrRadicada;
import com.trackflow.modules.pqr.domain.EstadoPqr;
import com.trackflow.modules.pqr.domain.TipoPqr;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificadorDePqrPorCorreoTest {

    private static final Instant AHORA = Instant.parse("2026-11-19T15:00:00Z");

    private final EnvioDeCorreo correo = mock(EnvioDeCorreo.class);
    private final NotificadorDePqrPorCorreo notificador = new NotificadorDePqrPorCorreo(correo);

    @Test
    void al_radicar_manda_el_radicado_y_nada_del_caso() {
        notificador.alRadicar(new PqrRadicada("PQR-2026-00042", TipoPqr.RECLAMO, "Ana\nPérez", "ana@correo.com",
                AHORA));

        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(correo).enviar(org.mockito.ArgumentMatchers.eq("ana@correo.com"),
                org.mockito.ArgumentMatchers.eq(NotificadorDePqrPorCorreo.ASUNTO_RADICADA), cuerpo.capture());
        assertThat(cuerpo.getValue()).contains("PQR-2026-00042").contains("reclamo").contains("Hola, Ana Pérez:");
    }

    @Test
    void al_cambiar_el_estado_avisa_el_nuevo_estado() {
        notificador.alCambiarElEstado(new PqrEstadoCambiado("PQR-2026-00042", EstadoPqr.EN_TRAMITE,
                EstadoPqr.RESUELTA, "Ana", "ana@correo.com", AHORA));

        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(correo).enviar(org.mockito.ArgumentMatchers.eq("ana@correo.com"),
                org.mockito.ArgumentMatchers.eq(NotificadorDePqrPorCorreo.ASUNTO_ACTUALIZADA), cuerpo.capture());
        assertThat(cuerpo.getValue()).contains("PQR-2026-00042").contains("RESUELTA");
    }

    @Test
    void si_el_proveedor_falla_no_se_propaga() {
        doThrow(new IllegalStateException("proveedor caído")).when(correo).enviar(anyString(), anyString(),
                anyString());

        notificador.alRadicar(new PqrRadicada("PQR-2026-00042", TipoPqr.QUEJA, "Ana", "ana@correo.com", AHORA));

        verify(correo).enviar(anyString(), anyString(), anyString());
    }
}
