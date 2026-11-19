package com.trackflow.modules.chatbot.infrastructure;

import com.trackflow.modules.chatbot.application.CatalogoDeFaq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Carga las preguntas frecuentes al arrancar. Si la base no responde en ese momento,
 * la aplicación arranca igual y el chatbot contesta "fuera de tema" hasta que un
 * administrador cambie algo: es mejor que no poder arrancar por un chat.
 */
@Component
public class CargadorDeFaq {

    private static final Logger log = LoggerFactory.getLogger(CargadorDeFaq.class);

    private final CatalogoDeFaq catalogo;

    public CargadorDeFaq(CatalogoDeFaq catalogo) {
        this.catalogo = catalogo;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void cargar() {
        try {
            catalogo.recargar();
        } catch (RuntimeException e) {
            log.warn("No se pudieron cargar las preguntas frecuentes del chatbot: {}", e.getMessage());
        }
    }
}
