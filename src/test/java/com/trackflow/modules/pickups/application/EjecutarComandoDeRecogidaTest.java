package com.trackflow.modules.pickups.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.trackflow.modules.pickups.domain.PackageSize;
import com.trackflow.modules.pickups.domain.Pickup;
import com.trackflow.modules.pickups.domain.PickupNumber;
import com.trackflow.modules.pickups.domain.PickupEvent;
import com.trackflow.modules.pickups.domain.PickupEventType;
import com.trackflow.modules.pickups.domain.PickupStatus;
import com.trackflow.modules.pickups.domain.EnvioYaVinculadoException;
import com.trackflow.modules.pickups.domain.EnvioYaEnRecorridoException;
import com.trackflow.shared.events.PickupCompletedEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

class EjecutarComandoDeRecogidaTest {
    private static final String NUMBER = "RC0123456789AB";
    private static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");
    private final PickupRepository repository = mock(PickupRepository.class);
    private final PickupEventRepository events = mock(PickupEventRepository.class);
    private final RecogidaCompletadaPublisher publisher = mock(RecogidaCompletadaPublisher.class);
    private final EnviosRegistrados envios = mock(EnviosRegistrados.class);
    private final EjecutarComandoDeRecogida service = new EjecutarComandoDeRecogida(repository, events, publisher, envios);

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

    @Test
    void guardaHashAlCrearYDetalleAnteriorAlReprogramar() {
        RecogidaComando crear = new RecogidaComando("e1", RecogidaComando.Tipo.CREAR, NUMBER, "remitente", NOW,
                "Calle 1", 1L, "Ana", "3001234567", LocalDate.of(2026, 11, 17), "08:00-10:00",
                PackageSize.MEDIUM, BigDecimal.ONE, null, null, null, "$2a$hash");
        service.ejecutar(crear);
        ArgumentCaptor<Pickup> pickup = ArgumentCaptor.forClass(Pickup.class);
        verify(repository).save(pickup.capture());
        assertEquals("$2a$hash", pickup.getValue().getManagementCodeHash());
        when(repository.findByNumberForUpdate(NUMBER)).thenReturn(Optional.of(pickup.getValue()));
        service.ejecutar(new RecogidaComando("e2", RecogidaComando.Tipo.REPROGRAMAR, NUMBER, "remitente", NOW,
                null, null, null, null, LocalDate.of(2026, 11, 18), "10:00-12:00", null, null, null, null, null));
        ArgumentCaptor<PickupEvent> evento = ArgumentCaptor.forClass(PickupEvent.class);
        verify(events, times(2)).save(evento.capture());
        assertEquals(PickupEventType.RESCHEDULED, evento.getAllValues().getLast().getType());
        assertEquals("De 2026-11-17 08:00-10:00 a 2026-11-18 10:00-12:00",
                evento.getAllValues().getLast().getDetail());
    }

    @Test
    void creaSinGuiaSiYaEstaVinculada() {
        when(repository.existsActiveByShipmentExcept("TF0123456789AB", null)).thenReturn(true);
        RecogidaComando crear = new RecogidaComando("e1", RecogidaComando.Tipo.CREAR, NUMBER, "remitente", NOW,
                "Calle 1", 1L, "Ana", "3001234567", LocalDate.of(2026, 11, 17), "08:00-10:00",
                PackageSize.MEDIUM, BigDecimal.ONE, "TF0123456789AB", null, null);
        service.ejecutar(crear);
        ArgumentCaptor<Pickup> pickup = ArgumentCaptor.forClass(Pickup.class);
        verify(repository).save(pickup.capture());
        assertNull(pickup.getValue().getShipmentTrackingNumber());
        ArgumentCaptor<PickupEvent> evento = ArgumentCaptor.forClass(PickupEvent.class);
        verify(events, times(2)).save(evento.capture());
        assertEquals(PickupEventType.CREATED, evento.getAllValues().getFirst().getType());
        assertEquals(PickupEventType.REJECTED, evento.getAllValues().getLast().getType());
        assertEquals("e1:guia", evento.getAllValues().getLast().getEventId());
    }

    @Test
    void consumidorRevalidaGuiaEnCreacionYVinculacion() {
        String guia = "TF0123456789AB";
        RecogidaComando crear = new RecogidaComando("e1", RecogidaComando.Tipo.CREAR, NUMBER, "operador", NOW,
                "Calle 1", 1L, "Ana", "3001234567", LocalDate.of(2026, 11, 17), "08:00-10:00",
                PackageSize.MEDIUM, BigDecimal.ONE, guia, null, null);
        recogida(null);
        RecogidaComando vincular = command(RecogidaComando.Tipo.VINCULAR, guia);
        doThrow(new EnvioYaEnRecorridoException(guia)).when(envios).exigir(guia, 1L);
        service.ejecutar(crear);
        service.ejecutar(vincular);
        doNothing().when(envios).exigir(guia, 1L);
        when(repository.existsActiveByShipmentExcept(guia, null)).thenReturn(true);
        when(repository.existsActiveByShipmentExcept(guia, NUMBER)).thenReturn(true);
        service.ejecutar(crear);
        service.ejecutar(vincular);
        verify(repository, times(2)).save(any());
        ArgumentCaptor<PickupEvent> evento = ArgumentCaptor.forClass(PickupEvent.class);
        verify(events, times(6)).save(evento.capture());
        assertEquals(4, evento.getAllValues().stream().filter(e -> e.getType() == PickupEventType.REJECTED).count());
        verifyNoInteractions(publisher);
    }

    @Test
    void cancelacionEnCaminoSeRechazaUnaSolaVezSinCambiarEstado() {
        Pickup pickup = recogida(null);
        Set<String> ids = new HashSet<>();
        when(events.existsByEventId(any())).thenAnswer(inv -> ids.contains(inv.getArgument(0)));
        when(events.save(any())).thenAnswer(inv -> {
            PickupEvent event = inv.getArgument(0);
            ids.add(event.getEventId());
            return event;
        });
        RecogidaComando cancelar = command(RecogidaComando.Tipo.CANCELAR, null);
        service.ejecutar(cancelar);
        service.ejecutar(cancelar);
        ArgumentCaptor<PickupEvent> evento = ArgumentCaptor.forClass(PickupEvent.class);
        verify(events).save(evento.capture());
        assertEquals(PickupEventType.REJECTED, evento.getValue().getType());
        assertEquals(PickupStatus.ON_THE_WAY, evento.getValue().getPreviousStatus());
        assertEquals(PickupStatus.ON_THE_WAY, evento.getValue().getNewStatus());
        assertEquals("operador", evento.getValue().getActorUsername());
        assertTrue(evento.getValue().getDetail().startsWith("CANCELAR: "));
        assertEquals(PickupStatus.ON_THE_WAY, pickup.getStatus());
        verify(repository, never()).save(any());
        verifyNoInteractions(publisher);
    }

    @Test
    void crearConGuiaAvanzadaConservaHashYEventoIdLargoAcotado() {
        String id = "x".repeat(255);
        String guia = "TF0123456789AB";
        doThrow(new EnvioYaEnRecorridoException(guia)).when(envios).exigir(guia, 1L);
        RecogidaComando crear = new RecogidaComando(id, RecogidaComando.Tipo.CREAR, NUMBER, "remitente", NOW,
                "Calle 1", 1L, "Ana", "3001234567", LocalDate.of(2026, 11, 17), "08:00-10:00",
                PackageSize.MEDIUM, BigDecimal.ONE, guia, null, null, "hash");
        service.ejecutar(crear);
        ArgumentCaptor<Pickup> pickup = ArgumentCaptor.forClass(Pickup.class);
        verify(repository).save(pickup.capture());
        assertNull(pickup.getValue().getShipmentTrackingNumber());
        assertEquals("hash", pickup.getValue().getManagementCodeHash());
        ArgumentCaptor<PickupEvent> eventos = ArgumentCaptor.forClass(PickupEvent.class);
        verify(events, times(2)).save(eventos.capture());
        PickupEvent rechazo = eventos.getAllValues().getLast();
        assertEquals(PickupEventType.REJECTED, rechazo.getType());
        assertTrue(rechazo.getEventId().length() <= 255);
        assertTrue(rechazo.getDetail().contains(guia));
        assertEquals(PickupStatus.REQUESTED, rechazo.getPreviousStatus());
        assertEquals(PickupStatus.REQUESTED, rechazo.getNewStatus());
    }

    @Test
    void erroresTecnicosYConflictosDeIndiceSePropagan() {
        RecogidaComando crear = new RecogidaComando("e1", RecogidaComando.Tipo.CREAR, NUMBER, "operador", NOW,
                "Calle 1", 1L, "Ana", "3001234567", LocalDate.of(2026, 11, 17), "08:00-10:00",
                PackageSize.MEDIUM, BigDecimal.ONE, "TF0123456789AB", null, null);
        doThrow(new DataIntegrityViolationException("unique index")).when(repository).save(any());
        assertThrows(DataIntegrityViolationException.class, () -> service.ejecutar(crear));
        verify(events, never()).save(any());
    }

    @Test
    void falloDePersistenciaNoSeConvierteEnRechazo() {
        recogida(null);
        doThrow(new IllegalStateException("fallo tecnico")).when(repository).save(any());
        assertThrows(IllegalStateException.class,
                () -> service.ejecutar(command(RecogidaComando.Tipo.COMPLETAR, null)));
        verify(events, never()).save(any());
        verifyNoInteractions(publisher);
    }

    @Test
    void comandoDeRecogidaInexistenteDejaRechazoYNoPropaga() {
        when(repository.findByNumberForUpdate(NUMBER)).thenReturn(Optional.empty());
        service.ejecutar(command(RecogidaComando.Tipo.CANCELAR, null));
        ArgumentCaptor<PickupEvent> evento = ArgumentCaptor.forClass(PickupEvent.class);
        verify(events).save(evento.capture());
        assertEquals(PickupEventType.REJECTED, evento.getValue().getType());
        assertEquals(NUMBER, evento.getValue().getPickupNumber());
        assertNull(evento.getValue().getPreviousStatus());
        assertNull(evento.getValue().getNewStatus());
        assertTrue(evento.getValue().getDetail().startsWith("CANCELAR: "));
        verify(repository, never()).save(any());
        verifyNoInteractions(publisher);
    }
}
