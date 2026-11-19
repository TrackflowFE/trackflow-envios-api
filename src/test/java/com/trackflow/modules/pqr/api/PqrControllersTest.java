package com.trackflow.modules.pqr.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackflow.bootstrap.ValidationExceptionHandler;
import com.trackflow.modules.pqr.application.ConsultarPqr;
import com.trackflow.modules.pqr.application.GestionarPqr;
import com.trackflow.modules.pqr.application.RadicarPqr;
import com.trackflow.modules.pqr.application.VistaDePqr;
import com.trackflow.modules.pqr.domain.EstadoPqr;
import com.trackflow.modules.pqr.domain.Pqr;
import com.trackflow.modules.pqr.domain.PqrNoEncontradaException;
import com.trackflow.modules.pqr.domain.TipoPqr;
import com.trackflow.modules.pqr.domain.TransicionDePqrInvalidaException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PqrControllersTest {

    private static final Instant AHORA = Instant.parse("2026-11-19T15:00:00Z");
    private static final String CUERPO = """
            {"tipo":"RECLAMO","descripcion":"El paquete llegó golpeado y abierto.",
             "contactoNombre":"Ana Pérez","contactoEmail":"ana@correo.com","contactoTelefono":"3001234567",
             "numeroSeguimiento":"TFABC123DEF456","aceptaTratamientoDatos":true}
            """;

    private final RadicarPqr radicar = mock(RadicarPqr.class);
    private final ConsultarPqr consultar = mock(ConsultarPqr.class);
    private final GestionarPqr gestionar = mock(GestionarPqr.class);
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new PqrPublicController(radicar, consultar),
                    new PqrGestionController(consultar, gestionar))
            .setControllerAdvice(new ValidationExceptionHandler(), new PqrExceptionHandler()).build();

    private static Pqr pqr() {
        return Pqr.radicar("PQR-2026-00042", TipoPqr.RECLAMO, "<script>alert(1)</script> descripción larga",
                new Pqr.Contacto("Ana Pérez", "ana@correo.com", "3001234567"), "TFABC123DEF456", true, AHORA);
    }

    private static VistaDePqr vista(Pqr pqr) {
        return new VistaDePqr(pqr, List.of(pqr.eventoDeRadicacion()));
    }

    @Test
    void radicar_devuelve_201_con_el_radicado() throws Exception {
        when(radicar.radicar(any())).thenReturn(pqr());

        mvc.perform(post("/api/pqr").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.radicado").value("PQR-2026-00042"))
                .andExpect(jsonPath("$.estado").value("RECIBIDA"));
    }

    @Test
    void radicar_rechaza_datos_invalidos_sin_llamar_al_caso_de_uso() throws Exception {
        String[] malos = {
                CUERPO.replace("\"aceptaTratamientoDatos\":true", "\"aceptaTratamientoDatos\":false"),
                CUERPO.replace("RECLAMO", "SUGERENCIA"),
                CUERPO.replace("ana@correo.com", "no-es-correo"),
                CUERPO.replace("TFABC123DEF456", "ABC"),
                CUERPO.replace("El paquete llegó golpeado y abierto.", "corta"),
                CUERPO.replace("3001234567", "abc")
        };
        for (String malo : malos) {
            mvc.perform(post("/api/pqr").contentType(MediaType.APPLICATION_JSON).content(malo))
                    .andExpect(status().isBadRequest());
        }
        verify(radicar, never()).radicar(any());
    }

    @Test
    void consulta_del_solicitante_oculta_contacto_y_respuesta_sin_resolver() throws Exception {
        when(consultar.delSolicitante("PQR-2026-00042", "ana@correo.com")).thenReturn(vista(pqr()));

        mvc.perform(post("/api/pqr/consulta").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"radicado\":\"PQR-2026-00042\",\"email\":\"ana@correo.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECIBIDA"))
                .andExpect(jsonPath("$.respuesta").doesNotExist())
                .andExpect(jsonPath("$.contactoEmail").doesNotExist())
                .andExpect(jsonPath("$.contactoTelefono").doesNotExist())
                .andExpect(jsonPath("$.historial[0].estado").value("RECIBIDA"));
    }

    @Test
    void la_consulta_entrega_el_texto_peligroso_solo_como_json() throws Exception {
        Pqr pqr = pqr();
        pqr.cambiarEstado(EstadoPqr.EN_TRAMITE, null, "s", AHORA);
        pqr.cambiarEstado(EstadoPqr.RESUELTA, "<img src=x onerror=alert(1)> respuesta", "s", AHORA);
        when(consultar.delSolicitante(anyString(), anyString())).thenReturn(new VistaDePqr(pqr, List.of()));

        mvc.perform(post("/api/pqr/consulta").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"radicado\":\"PQR-2026-00042\",\"email\":\"ana@correo.com\"}"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.respuesta").value("<img src=x onerror=alert(1)> respuesta"));
    }

    @Test
    void consulta_que_no_coincide_da_404_y_sin_datos_da_400() throws Exception {
        when(consultar.delSolicitante(anyString(), anyString())).thenThrow(new PqrNoEncontradaException());

        mvc.perform(post("/api/pqr/consulta").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"radicado\":\"PQR-2026-00042\",\"email\":\"otra@correo.com\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/pqr/consulta").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void la_bandeja_acota_la_pagina_y_el_tamano() throws Exception {
        when(consultar.listar(any(), any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of(pqr()));

        mvc.perform(get("/api/pqr").param("estado", "RECIBIDA").param("page", "-3").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].radicado").value("PQR-2026-00042"))
                .andExpect(jsonPath("$[0].contactoEmail").doesNotExist());
        verify(consultar).listar(EstadoPqr.RECIBIDA, null, 0, 50);
    }

    @Test
    void el_detalle_muestra_el_caso_completo_a_quien_atiende() throws Exception {
        when(consultar.detalle("PQR-2026-00042")).thenReturn(vista(pqr()));

        mvc.perform(get("/api/pqr/PQR-2026-00042"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactoEmail").value("ana@correo.com"))
                .andExpect(jsonPath("$.descripcion").value("<script>alert(1)</script> descripción larga"));
    }

    @Test
    void cambiar_estado_registra_al_usuario_del_token() throws Exception {
        Pqr pqr = pqr();
        when(gestionar.cambiarEstado(eq("PQR-2026-00042"), eq(EstadoPqr.EN_TRAMITE), any(), eq("supervisor")))
                .thenReturn(vista(pqr));

        mvc.perform(post("/api/pqr/PQR-2026-00042/estado").principal(() -> "supervisor")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"EN_TRAMITE\"}"))
                .andExpect(status().isOk());
        verify(gestionar).cambiarEstado("PQR-2026-00042", EstadoPqr.EN_TRAMITE, null, "supervisor");
    }

    @Test
    void sin_token_el_actor_es_el_local() throws Exception {
        when(gestionar.cambiarEstado(anyString(), any(), any(), eq("sistema-local"))).thenReturn(vista(pqr()));

        mvc.perform(post("/api/pqr/PQR-2026-00042/estado").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_TRAMITE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void transicion_prohibida_da_409_y_estado_ausente_da_400() throws Exception {
        when(gestionar.cambiarEstado(anyString(), any(), any(), anyString()))
                .thenThrow(new TransicionDePqrInvalidaException("PQR-2026-00042", EstadoPqr.RECIBIDA,
                        EstadoPqr.CERRADA));

        mvc.perform(post("/api/pqr/PQR-2026-00042/estado").principal(() -> "admin")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CERRADA\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/pqr/PQR-2026-00042/estado").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void falta_de_respuesta_y_argumentos_invalidos_dan_400() throws Exception {
        when(gestionar.cambiarEstado(anyString(), any(), any(), anyString()))
                .thenThrow(new com.trackflow.modules.pqr.domain.RespuestaRequeridaException())
                .thenThrow(new IllegalArgumentException("dato inválido"));

        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/pqr/PQR-2026-00042/estado").principal(() -> "admin")
                            .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"RESUELTA\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

}
