package com.trackflow.modules.pickups.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.*;

import com.trackflow.modules.pickups.application.RecogidaComando;
import com.trackflow.modules.pickups.domain.PackageSize;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

class RecogidaComandoMensajeTest {
    @Test
    void mensajeJsonYComandoTransportanHashSinCodigoPlano() {
        RecogidaComando comando = new RecogidaComando("e1", RecogidaComando.Tipo.CREAR, "RC0123456789AB",
                "remitente", Instant.parse("2026-11-16T12:00:00Z"), "Calle 1", 1L, "Ana", "3001234567",
                LocalDate.of(2026, 11, 17), "08:00-10:00", PackageSize.MEDIUM, BigDecimal.ONE,
                null, null, null, "$2a$hash");
        RecogidaComandoMensaje mensaje = RecogidaComandoMensaje.from(comando);
        JacksonJsonMessageConverter conversor = new JacksonJsonMessageConverter("com.trackflow.modules.pickups.infrastructure.messaging");
        var amqp = conversor.toMessage(mensaje, new MessageProperties());
        String json = new String(amqp.getBody(), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"managementCodeHash\":\"$2a$hash\""), json);
        assertFalse(json.contains("\"managementCode\":"), json);
        assertFalse(json.contains("ABCDEFGH"), json);
        RecogidaComandoMensaje recibido = (RecogidaComandoMensaje) conversor.fromMessage(amqp);
        assertEquals(comando, recibido.toComando());
    }
}
