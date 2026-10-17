package com.trackflow.modules.shipments.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.trackflow.modules.shipments.application.EnvioSolicitado;
import com.trackflow.modules.shipments.domain.Party;
import com.trackflow.modules.shipments.domain.TipoDocumento;
import com.trackflow.shared.geografia.Ciudad;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitMQEnvioSolicitadoPublisherTest {

    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final RabbitMQEnvioSolicitadoPublisher publicador = new RabbitMQEnvioSolicitadoPublisher(rabbit);

    @Test
    void publicaElMensajeEnElIntercambioPrivadoConLaClaveDeSolicitud() {
        EnvioSolicitado solicitud = new EnvioSolicitado("evt-1", "TF0123456789AB",
                new Party("Ana", TipoDocumento.CC, "1000000001", "300", "Calle 1", 1L),
                new Party("Luis", TipoDocumento.CC, "2000000002", "301", "Carrera 2", 10L),
                new Ciudad(1L, "MEDELLÍN", "ANTIOQUIA"), new Ciudad(10L, "BOGOTÁ D.C.", "BOGOTÁ"),
                "Documentos", Instant.parse("2026-10-15T18:00:00Z"));

        publicador.publicar(solicitud);

        ArgumentCaptor<Object> mensaje = ArgumentCaptor.forClass(Object.class);
        verify(rabbit).convertAndSend(eq("shipments.topic"), eq("shipment.requested"), mensaje.capture());
        assertEquals(EnvioSolicitadoMensaje.from(solicitud), mensaje.getValue());
    }
}
