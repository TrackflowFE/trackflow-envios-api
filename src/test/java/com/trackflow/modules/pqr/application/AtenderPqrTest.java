package com.trackflow.modules.pqr.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trackflow.modules.pqr.domain.EstadoPqr;
import com.trackflow.modules.pqr.domain.Pqr;
import com.trackflow.modules.pqr.domain.PqrNoEncontradaException;
import com.trackflow.modules.pqr.domain.RespuestaRequeridaException;
import com.trackflow.modules.pqr.domain.TipoPqr;
import com.trackflow.modules.pqr.domain.TransicionDePqrInvalidaException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AtenderPqrTest {

    private static final String RADICADO = "PQR-2026-00042";
    private static final String RESPUESTA = "Revisamos su caso y el paquete fue reenviado.";

    private final PqrEnMemoria repositorio = new PqrEnMemoria();
    private final PublicadorGrabador publicador = new PublicadorGrabador();
    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-19T15:00:00Z"), ZoneOffset.UTC);
    private final ConsultarPqr consultar = new ConsultarPqr(repositorio);
    private final GestionarPqr gestionar = new GestionarPqr(repositorio, publicador, reloj);

    @BeforeEach
    void pqrRadicada() {
        new RadicarPqr(repositorio, () -> RADICADO, publicador, reloj).radicar(new RadicarPqr.Solicitud(
                TipoPqr.QUEJA, "La atención en el punto fue muy deficiente.", "Ana Pérez", "ana@correo.com", null,
                null, true));
    }

    @Test
    void el_solicitante_consulta_con_radicado_y_correo_sin_importar_mayusculas() {
        VistaDePqr vista = consultar.delSolicitante(" pqr-2026-00042 ", "  ANA@Correo.com");

        assertThat(vista.pqr().radicado()).isEqualTo(RADICADO);
        assertThat(vista.historial()).hasSize(1);
    }

    @Test
    void correo_ajeno_y_radicado_inexistente_fallan_igual() {
        Throwable ajeno = org.assertj.core.api.Assertions.catchThrowable(
                () -> consultar.delSolicitante(RADICADO, "otra@correo.com"));
        Throwable inexistente = org.assertj.core.api.Assertions.catchThrowable(
                () -> consultar.delSolicitante("PQR-2026-99999", "ana@correo.com"));
        Throwable sinDatos = org.assertj.core.api.Assertions.catchThrowable(() -> consultar.delSolicitante(null, null));

        assertThat(ajeno).isInstanceOf(PqrNoEncontradaException.class);
        assertThat(inexistente).isInstanceOf(PqrNoEncontradaException.class).hasMessage(ajeno.getMessage());
        assertThat(sinDatos).isInstanceOf(PqrNoEncontradaException.class);
    }

    @Test
    void quien_atiende_responde_y_el_solicitante_recibe_aviso() {
        gestionar.cambiarEstado(RADICADO, EstadoPqr.EN_TRAMITE, null, "supervisor");
        VistaDePqr vista = gestionar.cambiarEstado(RADICADO, EstadoPqr.RESUELTA, RESPUESTA, "supervisor");

        assertThat(vista.pqr().estado()).isEqualTo(EstadoPqr.RESUELTA);
        assertThat(vista.pqr().respuesta()).isEqualTo(RESPUESTA);
        assertThat(vista.historial()).hasSize(3);
        assertThat(publicador.cambios).hasSize(2);
        assertThat(publicador.cambios.get(1).anterior()).isEqualTo(EstadoPqr.EN_TRAMITE);
        assertThat(publicador.cambios.get(1).nuevo()).isEqualTo(EstadoPqr.RESUELTA);
        assertThat(publicador.cambios.get(1).email()).isEqualTo("ana@correo.com");
    }

    @Test
    void una_transicion_prohibida_o_sin_respuesta_no_cambia_nada_ni_avisa() {
        assertThatThrownBy(() -> gestionar.cambiarEstado(RADICADO, EstadoPqr.CERRADA, null, "supervisor"))
                .isInstanceOf(TransicionDePqrInvalidaException.class);
        gestionar.cambiarEstado(RADICADO, EstadoPqr.EN_TRAMITE, null, "supervisor");
        assertThatThrownBy(() -> gestionar.cambiarEstado(RADICADO, EstadoPqr.RESUELTA, null, "supervisor"))
                .isInstanceOf(RespuestaRequeridaException.class);

        assertThat(repositorio.pqrs.get(0).estado()).isEqualTo(EstadoPqr.EN_TRAMITE);
        assertThat(publicador.cambios).hasSize(1);
    }

    @Test
    void cambiar_el_estado_de_un_radicado_inexistente_falla() {
        assertThatThrownBy(() -> gestionar.cambiarEstado("PQR-2026-99999", EstadoPqr.EN_TRAMITE, null, "s"))
                .isInstanceOf(PqrNoEncontradaException.class);
    }

    @Test
    void la_bandeja_filtra_por_estado_y_por_tipo() {
        assertThat(consultar.listar(null, null, 0, 20)).hasSize(1);
        assertThat(consultar.listar(EstadoPqr.RECIBIDA, TipoPqr.QUEJA, 0, 20)).hasSize(1);
        assertThat(consultar.listar(EstadoPqr.RESUELTA, null, 0, 20)).isEmpty();
        assertThat(consultar.listar(null, TipoPqr.RECLAMO, 0, 20)).isEmpty();
        Pqr detalle = consultar.detalle(RADICADO).pqr();
        assertThat(detalle.contactoNombre()).isEqualTo("Ana Pérez");
        assertThatThrownBy(() -> consultar.detalle("PQR-2026-99999")).isInstanceOf(PqrNoEncontradaException.class);
    }
}
