package com.trackflow.modules.shipments.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.trackflow.modules.shipments.application.EnvioSolicitado;
import com.trackflow.modules.shipments.application.RegistrarEnvio;
import com.trackflow.modules.shipments.domain.DocumentoInvalidoException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RabbitMQEnvioSolicitadoListenerTest {

    private final RegistrarEnvio registrar = mock(RegistrarEnvio.class);
    private final RabbitMQEnvioSolicitadoListener listener = new RabbitMQEnvioSolicitadoListener(registrar);

    private static EnvioSolicitadoMensaje mensaje(String documentoRemitente) {
        return new EnvioSolicitadoMensaje("evt-1", "TF0123456789AB",
                new EnvioSolicitadoMensaje.PersonaMensaje("Ana", "CC", documentoRemitente, "300", "Calle 1", 1L),
                new EnvioSolicitadoMensaje.PersonaMensaje("Luis", "CC", "2000000002", "301", "Carrera 2", 10L),
                new EnvioSolicitadoMensaje.CiudadMensaje(1L, "MEDELLÍN", "ANTIOQUIA"),
                new EnvioSolicitadoMensaje.CiudadMensaje(10L, "BOGOTÁ D.C.", "BOGOTÁ"),
                "Documentos", Instant.parse("2026-10-15T18:00:00Z"));
    }

    @Test
    void registraElEnvioConLaSolicitudDelMensaje() {
        listener.recibir(mensaje("1000000001"));

        ArgumentCaptor<EnvioSolicitado> solicitud = ArgumentCaptor.forClass(EnvioSolicitado.class);
        verify(registrar).ejecutar(solicitud.capture());
        assertEquals("TF0123456789AB", solicitud.getValue().trackingNumber());
        assertEquals("Ana", solicitud.getValue().remitente().getFullName());
        assertEquals(10L, solicitud.getValue().ciudadDestino().id());
    }

    @Test
    void unMensajeInvalidoFallaSinRegistrarParaIrALaColaDeDescartados() {
        EnvioSolicitadoMensaje invalido = mensaje("XX");

        assertThrows(DocumentoInvalidoException.class, () -> listener.recibir(invalido));
        verifyNoInteractions(registrar);
    }
}
