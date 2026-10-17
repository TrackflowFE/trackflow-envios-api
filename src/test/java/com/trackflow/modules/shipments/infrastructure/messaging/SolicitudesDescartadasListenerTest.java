package com.trackflow.modules.shipments.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/** La cola de descartados solo deja rastro en el log: se verifica qué queda escrito. */
class SolicitudesDescartadasListenerTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(SolicitudesDescartadasListener.class);
    private final ListAppender<ILoggingEvent> registros = new ListAppender<>();
    private final SolicitudesDescartadasListener listener = new SolicitudesDescartadasListener();

    @BeforeEach
    void capturarLog() {
        registros.start();
        logger.addAppender(registros);
    }

    @AfterEach
    void soltarLog() {
        logger.detachAppender(registros);
    }

    @Test
    void registraComoErrorLosDatosQuePermitenRastrearLaSolicitud() {
        EnvioSolicitadoMensaje mensaje = new EnvioSolicitadoMensaje("evt-1", "TF0123456789AB", null,
                new EnvioSolicitadoMensaje.PersonaMensaje("Luis", "CC", "2000000002", "301", "Carrera 2", 10L),
                null, null, "Documentos", Instant.parse("2026-10-15T18:00:00Z"));

        listener.recibir(mensaje);

        assertEquals(1, registros.list.size());
        ILoggingEvent registro = registros.list.get(0);
        assertEquals(Level.ERROR, registro.getLevel());
        String texto = registro.getFormattedMessage();
        assertTrue(texto.contains("eventId=evt-1"), texto);
        assertTrue(texto.contains("trackingNumber=TF0123456789AB"), texto);
        assertTrue(texto.contains("recipient=Luis"), texto);
        assertTrue(texto.contains("requestedAt=2026-10-15T18:00:00Z"), texto);
    }

    @Test
    void unMensajeSinDestinatarioTambienQuedaRegistrado() {
        EnvioSolicitadoMensaje mensaje = new EnvioSolicitadoMensaje("evt-2", null, null, null, null, null, null,
                null);

        listener.recibir(mensaje);

        assertEquals(1, registros.list.size());
        assertTrue(registros.list.get(0).getFormattedMessage().contains("recipient=null"));
    }
}
