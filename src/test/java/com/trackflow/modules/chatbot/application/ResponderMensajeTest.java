package com.trackflow.modules.chatbot.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trackflow.modules.chatbot.application.RespuestaDelChat.Resultado;
import com.trackflow.modules.chatbot.application.RespuestaDelChat.TipoAccion;
import com.trackflow.modules.chatbot.domain.PreguntaFrecuente;
import com.trackflow.modules.chatbot.domain.PreguntaFrecuente.Contenido;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResponderMensajeTest {

    private final RelojManual reloj = new RelojManual();
    private final FaqEnMemoria repositorio = new FaqEnMemoria();
    private final CatalogoDeFaq catalogo = new CatalogoDeFaq(repositorio, 0.35);
    private final SesionesDeChat sesiones = new SesionesDeChat(3, 60, reloj);
    private final ResponderMensaje responder = new ResponderMensaje(sesiones, catalogo);

    @BeforeEach
    void contenido() {
        repositorio.save(PreguntaFrecuente.crear("como_rastrear", new Contenido("seguimiento",
                "¿Cómo rastreo mi envío?", List.of("rastreo", "rastrear", "guia"), "Usa la página de rastreo.", 1),
                "t", reloj.instant()));
        repositorio.save(PreguntaFrecuente.crear("radicar_pqr", new Contenido("pqr", "¿Cómo radico una PQR?",
                List.of("radicar", "pqr", "queja"), "Entra a PQR y cuéntanos tu caso.", 2), "t", reloj.instant()));
        repositorio.save(PreguntaFrecuente.crear("cobertura", new Contenido("cobertura", "¿A qué ciudades llegan?",
                List.of("ciudades", "cobertura"), "Mira el buscador de ciudades.", 3), "t", reloj.instant()));
        catalogo.recargar();
    }

    @Test
    void contesta_con_el_contenido_definido() {
        RespuestaDelChat respuesta = responder.responder(sesiones.crear(), "¿cómo rastreo mi envío?");

        assertEquals(Resultado.RESPUESTA, respuesta.resultado());
        assertEquals("como_rastrear", respuesta.intencion());
        assertEquals("Usa la página de rastreo.", respuesta.texto());
        assertEquals(TipoAccion.IR_A_CONSULTA, respuesta.acciones().get(0).tipo());
    }

    @Test
    void las_respuestas_de_pqr_ofrecen_radicar_y_las_demas_no_ofrecen_nada() {
        UUID sesion = sesiones.crear();

        assertEquals(TipoAccion.ABRIR_PQR, responder.responder(sesion, "como radico una queja").acciones().get(0).tipo());
        assertTrue(responder.responder(sesion, "a que ciudades llegan").acciones().isEmpty());
    }

    @Test
    void fuera_de_tema_lo_dice_y_ofrece_la_pqr() {
        RespuestaDelChat respuesta = responder.responder(sesiones.crear(), "quien gano el partido");

        assertEquals(Resultado.FUERA_DE_TEMA, respuesta.resultado());
        assertTrue(respuesta.texto().contains("fuera de lo que puedo responder"));
        assertEquals(List.of(TipoAccion.ABRIR_PQR), respuesta.acciones().stream().map(a -> a.tipo()).toList());
    }

    @Test
    void un_numero_de_guia_no_revela_nada_y_manda_a_la_consulta_oficial() {
        UUID sesion = sesiones.crear();
        String[] mensajes = {"mi guía TFA1B2C3D4E5F6 dónde está", "tfa1b2c3d4e5f6", "TF A1B2 C3D4 E5F6",
                "tf-a1b2-c3d4-e5f6 por favor", "TFZZZZZZZZZZZZ"};
        RespuestaDelChat primera = responder.responder(sesion, mensajes[0]);

        assertEquals(Resultado.NUMERO_DE_SEGUIMIENTO, primera.resultado());
        assertEquals(List.of(TipoAccion.IR_A_CONSULTA), primera.acciones().stream().map(a -> a.tipo()).toList());
        for (String mensaje : mensajes) {
            RespuestaDelChat respuesta = responder.responder(sesiones.crear(), mensaje);
            assertEquals(primera, respuesta, "la respuesta es idéntica exista o no el envío: " + mensaje);
            assertFalse(respuesta.texto().toUpperCase().contains("TF"), "no repite el número");
        }
    }

    @Test
    void no_confunde_palabras_corrientes_con_una_guia() {
        UUID sesion = sesiones.crear();

        for (String mensaje : new String[] {"left find nothing like it", "mi guia es TF123", "softforeverandever"}) {
            assertFalse(responder.responder(sesion, mensaje).resultado() == Resultado.NUMERO_DE_SEGUIMIENTO, mensaje);
        }
    }

    @Test
    void rechaza_sesiones_que_no_existen() {
        assertThrows(SesionInvalidaException.class, () -> responder.responder(UUID.randomUUID(), "hola"));
        assertThrows(SesionInvalidaException.class, () -> responder.responder(null, "hola"));
    }

    @Test
    void una_sesion_vence_a_las_dos_horas() {
        UUID sesion = sesiones.crear();

        reloj.avanzar(Duration.ofHours(2).plusSeconds(1));

        assertThrows(SesionInvalidaException.class, () -> responder.responder(sesion, "hola"));
    }

    @Test
    void rechaza_mensajes_vacios_o_demasiado_largos() {
        UUID sesion = sesiones.crear();

        assertThrows(IllegalArgumentException.class, () -> responder.responder(sesion, "   "));
        assertThrows(IllegalArgumentException.class, () -> responder.responder(sesion, null));
        assertThrows(IllegalArgumentException.class, () -> responder.responder(sesion, "a".repeat(301)));
        assertEquals(Resultado.FUERA_DE_TEMA, responder.responder(sesiones.crear(), "a".repeat(300)).resultado());
    }

    @Test
    void pasado_el_tope_rechaza_temporalmente_y_despues_vuelve_a_contestar() {
        UUID sesion = sesiones.crear();
        UUID otra = sesiones.crear();
        for (int i = 0; i < 3; i++) {
            responder.responder(sesion, "como rastreo mi envio");
        }

        DemasiadosMensajesException rechazo = assertThrows(DemasiadosMensajesException.class,
                () -> responder.responder(sesion, "como rastreo mi envio"));

        assertEquals(60, rechazo.segundos());
        assertEquals(Resultado.RESPUESTA, responder.responder(otra, "como rastreo mi envio").resultado());
        reloj.avanzar(Duration.ofSeconds(61));
        assertEquals(Resultado.RESPUESTA, responder.responder(sesion, "como rastreo mi envio").resultado());
    }

    @Test
    void rechaza_limites_invalidos() {
        assertThrows(IllegalArgumentException.class, () -> new SesionesDeChat(0, 60, reloj));
        assertThrows(IllegalArgumentException.class, () -> new SesionesDeChat(3, 0, reloj));
    }
}
