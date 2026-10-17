package com.trackflow.modules.shipments.infrastructure;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.trackflow.modules.shipments.application.AplicarEventoLogistico;
import com.trackflow.modules.shipments.domain.ShipmentNotFoundException;
import com.trackflow.shared.events.EventoLogisticoRegistradoEvent;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EventoLogisticoSubscriberTest {

    private static final Instant AHORA = Instant.parse("2026-10-15T18:00:00Z");

    private final AplicarEventoLogistico aplicar = mock(AplicarEventoLogistico.class);
    private final EventoLogisticoSubscriber subscriber = new EventoLogisticoSubscriber(aplicar);
    private final EventoLogisticoRegistradoEvent evento = new EventoLogisticoRegistradoEvent(7L, "TF0123456789AB",
            "RECEPCION", "AT_DISTRIBUTION_CENTER", "Centro", 3L, 1L, "MEDELLÍN", AHORA, AHORA, AHORA);

    @Test
    void entregaElEventoAlCasoDeUso() {
        subscriber.on(evento);

        verify(aplicar).ejecutar(evento);
    }

    @Test
    void dejaPropagarElErrorParaQueElMensajeVayaALaColaDeDescartados() {
        doThrow(new ShipmentNotFoundException("TF0123456789AB")).when(aplicar).ejecutar(evento);

        assertThrows(ShipmentNotFoundException.class, () -> subscriber.on(evento));
    }
}
