package com.trackflow.modules.pickups.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.trackflow.modules.pickups.domain.PackageSize;
import com.trackflow.modules.pickups.domain.Pickup;
import com.trackflow.modules.pickups.domain.PickupNumber;
import com.trackflow.shared.events.PickupCompletedEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EjecutarComandoDeRecogidaTest {
    private static final String NUMBER = "RC0123456789AB";
    private static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");
    private final PickupRepository repository = mock(PickupRepository.class);
    private final PickupEventRepository events = mock(PickupEventRepository.class);
    private final RecogidaCompletadaPublisher publisher = mock(RecogidaCompletadaPublisher.class);
    private final EjecutarComandoDeRecogida service = new EjecutarComandoDeRecogida(repository, events, publisher);

    private Pickup recogida(String tracking) {
        Pickup p = Pickup.registrar(PickupNumber.of(NUMBER), "Calle 1", 1L, "Ana", "3001234567",
                LocalDate.of(2026, 11, 17), "08:00-10:00", PackageSize.MEDIUM,
                BigDecimal.ONE, tracking, null, "operador", NOW);
        p.reprogramar(LocalDate.of(2026, 11, 17), "08:00-10:00", "operador", NOW);
        p.despachar(NOW);
        when(repository.findByNumberForUpdate(NUMBER)).thenReturn(Optional.of(p));
        return p;
    }

    private RecogidaComando command(RecogidaComando.Tipo type, String tracking) {
        return new RecogidaComando("event-1", type, NUMBER, "operador", NOW,
                null, null, null, null, null, null, null, null, tracking, null, null);
    }

    @Test
    void completarRegistraEventoDeCustodiaSinDatosPrivados() {
        recogida("TF0123456789AB");
        service.ejecutar(command(RecogidaComando.Tipo.COMPLETAR, null));
        ArgumentCaptor<PickupCompletedEvent> event = ArgumentCaptor.forClass(PickupCompletedEvent.class);
        verify(publisher).publicar(event.capture());
        assertEquals(NUMBER, event.getValue().pickupNumber());
        assertEquals("TF0123456789AB", event.getValue().trackingNumber());
        assertEquals(1L, event.getValue().cityId());
        verify(events).save(any());
    }

    @Test
    void sinGuiaSoloPublicaAlVincular() {
        recogida(null);
        service.ejecutar(command(RecogidaComando.Tipo.COMPLETAR, null));
        verifyNoInteractions(publisher);
        service.ejecutar(command(RecogidaComando.Tipo.VINCULAR, "TF0123456789AB"));
        verify(publisher).publicar(any());
    }

    @Test
    void noRepiteComandoAplicado() {
        when(events.existsByEventId("event-1")).thenReturn(true);
        service.ejecutar(command(RecogidaComando.Tipo.COMPLETAR, null));
        verifyNoInteractions(repository, publisher);
    }
}
