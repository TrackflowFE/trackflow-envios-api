package com.trackflow.modules.pqr.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class PqrTest {

    private static final Instant T0 = Instant.parse("2026-11-19T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-11-19T11:00:00Z");
    private static final String RESPUESTA = "Revisamos su caso y el paquete fue reenviado.";

    private static Pqr nueva() {
        return Pqr.radicar("PQR-2026-00001", TipoPqr.RECLAMO, "El paquete llegó golpeado y abierto.",
                new Pqr.Contacto("Ana Pérez", "  Ana@Correo.com ", null), "TFABC123DEF456", true, T0);
    }

    @Test
    void nace_recibida_con_el_correo_normalizado() {
        Pqr pqr = nueva();

        assertThat(pqr.estado()).isEqualTo(EstadoPqr.RECIBIDA);
        assertThat(pqr.contactoEmail()).isEqualTo("ana@correo.com");
        assertThat(pqr.esDe("ANA@correo.com ")).isTrue();
        assertThat(pqr.esDe("otra@correo.com")).isFalse();
        assertThat(pqr.esDe(null)).isFalse();
    }

    @Test
    void sin_aceptar_el_tratamiento_de_datos_no_se_radica() {
        assertThatThrownBy(() -> Pqr.radicar("PQR-2026-00002", TipoPqr.QUEJA, "d",
                new Pqr.Contacto("A", "a@b.co", null), null, false, T0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recorre_el_camino_completo_y_deja_bitacora() {
        Pqr pqr = nueva();

        PqrEvento enTramite = pqr.cambiarEstado(EstadoPqr.EN_TRAMITE, null, "supervisor", T1);
        PqrEvento resuelta = pqr.cambiarEstado(EstadoPqr.RESUELTA, RESPUESTA, "supervisor", T1);
        pqr.cambiarEstado(EstadoPqr.CERRADA, null, "admin", T1);

        assertThat(enTramite.estadoAnterior()).isEqualTo(EstadoPqr.RECIBIDA);
        assertThat(resuelta.estadoNuevo()).isEqualTo(EstadoPqr.RESUELTA);
        assertThat(resuelta.actor()).isEqualTo("supervisor");
        assertThat(pqr.estado()).isEqualTo(EstadoPqr.CERRADA);
        assertThat(pqr.respuesta()).isEqualTo(RESPUESTA);
        assertThat(pqr.respondidoPor()).isEqualTo("supervisor");
        assertThat(pqr.updatedAt()).isEqualTo(T1);
    }

    @Test
    void resolver_exige_una_respuesta_con_largo_valido() {
        Pqr pqr = nueva();
        pqr.cambiarEstado(EstadoPqr.EN_TRAMITE, null, "supervisor", T1);

        assertThatThrownBy(() -> pqr.cambiarEstado(EstadoPqr.RESUELTA, null, "s", T1))
                .isInstanceOf(RespuestaRequeridaException.class);
        assertThatThrownBy(() -> pqr.cambiarEstado(EstadoPqr.RESUELTA, "corta", "s", T1))
                .isInstanceOf(RespuestaRequeridaException.class);
        assertThatThrownBy(() -> pqr.cambiarEstado(EstadoPqr.RESUELTA, "x".repeat(2001), "s", T1))
                .isInstanceOf(RespuestaRequeridaException.class);
        assertThat(pqr.estado()).isEqualTo(EstadoPqr.EN_TRAMITE);
    }

    @Test
    void no_se_puede_saltar_ni_retroceder_salvo_reabrir() {
        Pqr pqr = nueva();

        assertThatThrownBy(() -> pqr.cambiarEstado(EstadoPqr.RESUELTA, RESPUESTA, "s", T1))
                .isInstanceOf(TransicionDePqrInvalidaException.class);

        pqr.cambiarEstado(EstadoPqr.EN_TRAMITE, null, "s", T1);
        pqr.cambiarEstado(EstadoPqr.RESUELTA, RESPUESTA, "s", T1);
        pqr.cambiarEstado(EstadoPqr.EN_TRAMITE, null, "s", T1);

        assertThat(pqr.estado()).isEqualTo(EstadoPqr.EN_TRAMITE);
        assertThat(pqr.respuesta()).isEqualTo(RESPUESTA);
    }

    @Test
    void cerrada_es_final() {
        Pqr pqr = nueva();
        pqr.cambiarEstado(EstadoPqr.EN_TRAMITE, null, "s", T1);
        pqr.cambiarEstado(EstadoPqr.RESUELTA, RESPUESTA, "s", T1);
        pqr.cambiarEstado(EstadoPqr.CERRADA, null, "s", T1);

        assertThatThrownBy(() -> pqr.cambiarEstado(EstadoPqr.EN_TRAMITE, null, "s", T1))
                .isInstanceOf(TransicionDePqrInvalidaException.class);
    }

    @Test
    void la_radicacion_deja_su_primer_evento_sin_estado_anterior() {
        PqrEvento evento = nueva().eventoDeRadicacion();

        assertThat(evento.estadoAnterior()).isNull();
        assertThat(evento.estadoNuevo()).isEqualTo(EstadoPqr.RECIBIDA);
        assertThat(evento.occurredAt()).isEqualTo(T0);
    }

    @Test
    void solo_resuelta_y_cerrada_muestran_la_respuesta() {
        assertThat(EstadoPqr.RECIBIDA.muestraRespuesta()).isFalse();
        assertThat(EstadoPqr.EN_TRAMITE.muestraRespuesta()).isFalse();
        assertThat(EstadoPqr.RESUELTA.muestraRespuesta()).isTrue();
        assertThat(EstadoPqr.CERRADA.muestraRespuesta()).isTrue();
    }
}
