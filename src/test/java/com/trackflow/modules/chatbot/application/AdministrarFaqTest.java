package com.trackflow.modules.chatbot.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.trackflow.bootstrap.security.Auditorias;
import com.trackflow.modules.chatbot.domain.FaqNoEncontradaException;
import com.trackflow.modules.chatbot.domain.IntencionDuplicadaException;
import com.trackflow.modules.chatbot.domain.PreguntaFrecuente;
import com.trackflow.modules.chatbot.domain.PreguntaFrecuente.Contenido;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdministrarFaqTest {

    private final RelojManual reloj = new RelojManual();
    private final FaqEnMemoria repositorio = new FaqEnMemoria();
    private final CatalogoDeFaq catalogo = new CatalogoDeFaq(repositorio, 0.35);
    private final Auditorias auditorias = mock(Auditorias.class);
    private final AdministrarFaq administrar = new AdministrarFaq(repositorio, catalogo, auditorias, reloj);

    private static Contenido contenido(String respuesta) {
        return new Contenido("envios", "¿Hacen envíos nocturnos?", List.of("nocturno", "noche"), respuesta, 4);
    }

    private String respuestaPara(String mensaje) {
        return catalogo.mejor(mensaje).map(c -> c.entrada().respuesta()).orElse("ninguna");
    }

    @Test
    void crear_deja_el_contenido_disponible_sin_reiniciar_y_queda_auditado() {
        administrar.crear("nocturnos", contenido("Por ahora no."), "admin");

        assertEquals("Por ahora no.", respuestaPara("envios nocturnos"));
        verify(auditorias).registrar("admin", Auditorias.FAQ_CREADA, "nocturnos", "categoria=envios");
    }

    @Test
    void no_deja_repetir_la_intencion() {
        administrar.crear("nocturnos", contenido("Por ahora no."), "admin");

        assertThrows(IntencionDuplicadaException.class,
                () -> administrar.crear("nocturnos", contenido("Otra respuesta distinta."), "admin"));
        assertEquals(1, administrar.listar().size());
    }

    @Test
    void editar_cambia_lo_que_contesta_y_conserva_la_intencion() {
        Long id = administrar.crear("nocturnos", contenido("Por ahora no."), "admin").id();

        PreguntaFrecuente editada = administrar.editar(id, contenido("Sí, hay envíos nocturnos."), "otro");

        assertEquals("Sí, hay envíos nocturnos.", respuestaPara("envios nocturnos"));
        assertEquals("nocturnos", editada.intencion());
        verify(auditorias).registrar("otro", Auditorias.FAQ_EDITADA, "nocturnos", "categoria=envios");
    }

    @Test
    void desactivar_la_saca_del_chat_y_reactivar_la_devuelve_sin_borrar_nada() {
        Long id = administrar.crear("nocturnos", contenido("Por ahora no."), "admin").id();

        administrar.cambiarActivo(id, false, "admin");
        assertEquals("ninguna", respuestaPara("envios nocturnos"));
        assertEquals(1, administrar.listar().size());

        administrar.cambiarActivo(id, true, "admin");
        assertEquals("Por ahora no.", respuestaPara("envios nocturnos"));
        verify(auditorias).registrar("admin", Auditorias.FAQ_DESACTIVADA, "nocturnos", "categoria=envios");
        verify(auditorias).registrar("admin", Auditorias.FAQ_ACTIVADA, "nocturnos", "categoria=envios");
    }

    @Test
    void una_pregunta_que_no_existe_da_404_y_no_audita() {
        assertThrows(FaqNoEncontradaException.class, () -> administrar.editar(99L, contenido("Hola de nuevo."), "a"));
        assertThrows(FaqNoEncontradaException.class, () -> administrar.cambiarActivo(99L, false, "a"));
        verifyNoInteractions(auditorias);
    }
}
