package com.trackflow.modules.shipments.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;

class ConversorJsonDeMensajesTest {

    private final MessageConverter conversor = new ConversorJsonDeMensajes().jsonMessageConverter();

    @Test
    void losMensajesViajanComoJson() {
        EnvioSolicitadoMensaje mensaje = new EnvioSolicitadoMensaje("evt-1", "TF0123456789AB", null, null, null,
                null, "Documentos", Instant.parse("2026-10-15T18:00:00Z"));

        Message amqp = conversor.toMessage(mensaje, new MessageProperties());

        assertEquals(MessageProperties.CONTENT_TYPE_JSON, amqp.getMessageProperties().getContentType());
        String cuerpo = new String(amqp.getBody(), StandardCharsets.UTF_8);
        assertTrue(cuerpo.contains("\"eventId\":\"evt-1\""), cuerpo);
        assertTrue(cuerpo.contains("\"version\":1"), cuerpo);
    }
}
