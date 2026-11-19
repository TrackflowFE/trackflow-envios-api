package com.trackflow.modules.chatbot.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackflow.bootstrap.ValidationExceptionHandler;
import com.trackflow.modules.chatbot.application.AdministrarFaq;
import com.trackflow.modules.chatbot.application.CatalogoDeFaq;
import com.trackflow.modules.chatbot.application.DemasiadosMensajesException;
import com.trackflow.modules.chatbot.application.RespuestaDelChat;
import com.trackflow.modules.chatbot.application.RespuestaDelChat.Accion;
import com.trackflow.modules.chatbot.application.RespuestaDelChat.Resultado;
import com.trackflow.modules.chatbot.application.RespuestaDelChat.TipoAccion;
import com.trackflow.modules.chatbot.application.ResponderMensaje;
import com.trackflow.modules.chatbot.application.SesionInvalidaException;
import com.trackflow.modules.chatbot.application.SesionesDeChat;
import com.trackflow.modules.chatbot.domain.FaqNoEncontradaException;
import com.trackflow.modules.chatbot.domain.IntencionDuplicadaException;
import com.trackflow.modules.chatbot.domain.PreguntaFrecuente;
import com.trackflow.modules.chatbot.domain.PreguntaFrecuente.Contenido;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ChatbotControllersTest {

    private static final UUID SESION = UUID.fromString("3f1c2a4e-9b7d-4c1a-8e5f-0a1b2c3d4e5f");
    private static final Instant AHORA = Instant.parse("2026-11-19T15:00:00Z");

    private final SesionesDeChat sesiones = mock(SesionesDeChat.class);
    private final ResponderMensaje responder = mock(ResponderMensaje.class);
    private final CatalogoDeFaq catalogo = mock(CatalogoDeFaq.class);
    private final AdministrarFaq administrar = mock(AdministrarFaq.class);
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new ChatbotController(sesiones, responder, catalogo),
                    new ChatbotAdminController(administrar))
            .setControllerAdvice(new ValidationExceptionHandler(), new ChatbotExceptionHandler()).build();

    private static String mensaje(String texto) {
        return "{\"sesionId\":\"" + SESION + "\",\"texto\":" + texto + "}";
    }

    private static PreguntaFrecuente faq() {
        return PreguntaFrecuente.crear("nocturnos", new Contenido("envios", "¿Hacen envíos nocturnos?",
                List.of("nocturno"), "Por ahora no.", 4), "admin", AHORA);
    }

    private static final String FAQ_JSON = """
            {"intencion":"nocturnos","categoria":"envios","pregunta":"¿Hacen envíos nocturnos?",
             "palabrasClave":["nocturno","noche"],"respuesta":"Por ahora no.","orden":4}
            """;

    @Test
    void crear_sesion_devuelve_201_con_el_identificador() throws Exception {
        when(sesiones.crear()).thenReturn(SESION);

        mvc.perform(post("/api/chatbot/sesiones")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.sesionId").value(SESION.toString()));
    }

    @Test
    void mensaje_devuelve_la_respuesta_con_su_forma_fija() throws Exception {
        when(responder.responder(eq(SESION), eq("hola"))).thenReturn(new RespuestaDelChat(Resultado.FUERA_DE_TEMA,
                "fuera_de_tema", "Eso está fuera de lo que puedo responder.",
                List.of(new Accion(TipoAccion.ABRIR_PQR, "Radicar una PQR"))));

        mvc.perform(post("/api/chatbot/mensajes").contentType(MediaType.APPLICATION_JSON).content(mensaje("\"hola\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("FUERA_DE_TEMA"))
                .andExpect(jsonPath("$.intencion").value("fuera_de_tema"))
                .andExpect(jsonPath("$.acciones[0].tipo").value("ABRIR_PQR"))
                .andExpect(jsonPath("$.acciones[0].etiqueta").value("Radicar una PQR"));
    }

    @Test
    void mensajes_invalidos_dan_400_sin_llamar_al_caso_de_uso() throws Exception {
        String[] malos = {mensaje("\"\""), mensaje("\"   \""), mensaje("null"), mensaje("\"" + "a".repeat(301) + "\""),
                "{\"texto\":\"hola\"}", "{\"sesionId\":\"no-es-uuid\",\"texto\":\"hola\"}"};
        for (String malo : malos) {
            mvc.perform(post("/api/chatbot/mensajes").contentType(MediaType.APPLICATION_JSON).content(malo))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(responder);
    }

    @Test
    void sesion_inexistente_da_400() throws Exception {
        when(responder.responder(any(), any())).thenThrow(new SesionInvalidaException());

        mvc.perform(post("/api/chatbot/mensajes").contentType(MediaType.APPLICATION_JSON).content(mensaje("\"hola\"")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.title").value("Sesión no válida"));
    }

    @Test
    void pasado_el_tope_da_429_con_cuanto_esperar() throws Exception {
        when(responder.responder(any(), any())).thenThrow(new DemasiadosMensajesException(60));

        mvc.perform(post("/api/chatbot/mensajes").contentType(MediaType.APPLICATION_JSON).content(mensaje("\"hola\"")))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.title").value("Demasiados mensajes"));
    }

    @Test
    void sugerencias_devuelve_intencion_y_pregunta() throws Exception {
        when(catalogo.sugerencias(6)).thenReturn(List.of(new CatalogoDeFaq.Entrada(1L, "como_rastrear", "seguimiento",
                "¿Cómo rastreo mi envío?", "Usa rastreo.", 1, List.of(), Set.of())));

        mvc.perform(get("/api/chatbot/sugerencias")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].intencion").value("como_rastrear"))
                .andExpect(jsonPath("$[0].pregunta").value("¿Cómo rastreo mi envío?"))
                .andExpect(jsonPath("$[0].respuesta").doesNotExist());
    }

    @Test
    void admin_lista_crea_edita_y_activa_con_el_usuario_como_actor() throws Exception {
        when(administrar.listar()).thenReturn(List.of(faq()));
        when(administrar.crear(eq("nocturnos"), any(), eq("admin"))).thenReturn(faq());
        when(administrar.editar(anyLong(), any(), eq("admin"))).thenReturn(faq());
        when(administrar.cambiarActivo(anyLong(), anyBoolean(), eq("admin"))).thenReturn(faq());

        mvc.perform(get("/api/admin/chatbot/faq")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].intencion").value("nocturnos"))
                .andExpect(jsonPath("$[0].palabrasClave[0]").value("nocturno"));
        mvc.perform(post("/api/admin/chatbot/faq").principal(() -> "admin").contentType(MediaType.APPLICATION_JSON)
                .content(FAQ_JSON)).andExpect(status().isCreated());
        mvc.perform(put("/api/admin/chatbot/faq/7").principal(() -> "admin").contentType(MediaType.APPLICATION_JSON)
                .content(FAQ_JSON.replace("\"intencion\":\"nocturnos\",", ""))).andExpect(status().isOk());
        mvc.perform(post("/api/admin/chatbot/faq/7/activo").principal(() -> "admin")
                .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}")).andExpect(status().isOk());
        verify(administrar).cambiarActivo(7L, false, "admin");
    }

    @Test
    void admin_rechaza_contenido_invalido_sin_llamar_al_caso_de_uso() throws Exception {
        String[] malos = {FAQ_JSON.replace("nocturnos\",\"categoria", "Con Mayusculas\",\"categoria"),
                FAQ_JSON.replace("\"palabrasClave\":[\"nocturno\",\"noche\"]", "\"palabrasClave\":[]"),
                FAQ_JSON.replace("Por ahora no.", "x".repeat(1001)),
                FAQ_JSON.replace("\"orden\":4", "\"orden\":1000")};
        for (String malo : malos) {
            mvc.perform(post("/api/admin/chatbot/faq").contentType(MediaType.APPLICATION_JSON).content(malo))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/admin/chatbot/faq/7/activo").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(administrar);
    }

    @Test
    void admin_traduce_duplicada_a_409_y_faltante_a_404() throws Exception {
        when(administrar.crear(any(), any(), any())).thenThrow(new IntencionDuplicadaException("nocturnos"));
        when(administrar.editar(anyLong(), any(), any())).thenThrow(new FaqNoEncontradaException(9L));

        mvc.perform(post("/api/admin/chatbot/faq").contentType(MediaType.APPLICATION_JSON).content(FAQ_JSON))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/admin/chatbot/faq/9").contentType(MediaType.APPLICATION_JSON)
                .content(FAQ_JSON.replace("\"intencion\":\"nocturnos\",", ""))).andExpect(status().isNotFound());
    }
}
