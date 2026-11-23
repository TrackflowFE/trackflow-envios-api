package com.trackflow.modules.pickups.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trackflow.modules.pickups.domain.EnvioYaVinculadoException;
import com.trackflow.modules.pickups.domain.PackageSize;
import com.trackflow.modules.pickups.domain.Pickup;
import com.trackflow.modules.pickups.domain.PickupEvent;
import com.trackflow.modules.pickups.domain.PickupEventType;
import com.trackflow.modules.pickups.domain.PickupNumber;
import com.trackflow.modules.pickups.domain.PickupStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EjecutarComandoDeRecogidaTransicionesTest {

    private static final String NUMBER = "RC0123456789AB";
    private static final String GUIA = "TF0123456789AB";
    private static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 17);
    private static final String FRANJA = "08:00-10:00";

    private final PickupRepository repository = mock(PickupRepository.class);
    private final PickupEventRepository events = mock(PickupEventRepository.class);
    private final RecogidaCompletadaPublisher publisher = mock(RecogidaCompletadaPublisher.class);
    private final EnviosRegistrados envios = mock(EnviosRegistrados.class);
    private final EjecutarComandoDeRecogida service = new EjecutarComandoDeRecogida(repository, events, publisher,
            envios);

    private Pickup solicitada() {
        Pickup pickup = Pickup.registrar(PickupNumber.of(NUMBER), "Calle 1", 1L, "Ana", "3001234567", FECHA, FRANJA,
                PackageSize.MEDIUM, BigDecimal.ONE, null, null, "operador", NOW);
        when(repository.findByNumberForUpdate(NUMBER)).thenReturn(Optional.of(pickup));
        return pickup;
    }

    private Pickup programada() {
        Pickup pickup = solicitada();
        pickup.reprogramar(FECHA, FRANJA, "operador", NOW);
        return pickup;
    }

    private RecogidaComando comando(RecogidaComando.Tipo tipo) {
        return new RecogidaComando("event-1", tipo, NUMBER, "operador", NOW,
                null, null, null, null, null, null, null, null, null, null, null);
    }

    private RecogidaComando reprogramacion(LocalDate fecha, String franja, String responsable) {
        return new RecogidaComando("event-1", RecogidaComando.Tipo.REPROGRAMAR, NUMBER, "supervisor", NOW,
                null, null, null, null, fecha, franja, null, null, null, responsable, null);
    }

    private PickupEvent eventoGuardado() {
        ArgumentCaptor<PickupEvent> captor = ArgumentCaptor.forClass(PickupEvent.class);
        verify(events).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void crearUnaRecogidaQueYaExisteNoHaceNada() {
        RecogidaComando crear = new RecogidaComando("event-1", RecogidaComando.Tipo.CREAR, NUMBER, "operador", NOW,
                "Calle 1", 1L, "Ana", "3001234567", FECHA, FRANJA, PackageSize.MEDIUM, BigDecimal.ONE, null, null,
                null, "hash");
        when(repository.existsByNumber(NUMBER)).thenReturn(true);

        service.ejecutar(crear);

        verify(repository, never()).save(any());
        verify(events, never()).save(any());
    }

    @Test
    void asignarUnaRecogidaSinResponsableLoDejaEnElDetalle() {
        solicitada();

        service.ejecutar(reprogramacion(FECHA, FRANJA, "operador"));

        PickupEvent evento = eventoGuardado();
        assertEquals(PickupEventType.ASSIGNED, evento.getType());
        assertEquals(PickupStatus.SCHEDULED, evento.getNewStatus());
        assertTrue(evento.getDetail().contains("asignada a operador (antes sin asignar)"));
    }

    @Test
    void reasignarUnaRecogidaNombraAlResponsableAnterior() {
        programada();

        service.ejecutar(reprogramacion(FECHA, FRANJA, "otro-operador"));

        assertTrue(eventoGuardado().getDetail().contains("asignada a otro-operador (antes operador)"));
    }

    @Test
    void reprogramarSinCambiosSeRegistraComoRechazo() {
        programada();

        service.ejecutar(reprogramacion(FECHA, FRANJA, null));

        assertEquals(PickupEventType.REJECTED, eventoGuardado().getType());
        verify(repository, never()).save(any());
    }

    @Test
    void cancelarGuardaElMotivo() {
        Pickup pickup = programada();
        RecogidaComando cancelar = new RecogidaComando("event-1", RecogidaComando.Tipo.CANCELAR, NUMBER, "operador",
                NOW, null, null, null, null, null, null, null, null, null, null, "Ya no lo necesito");

        service.ejecutar(cancelar);

        assertEquals(PickupStatus.CANCELLED, pickup.getStatus());
        verify(repository).save(pickup);
        PickupEvent evento = eventoGuardado();
        assertEquals(PickupEventType.CANCELLED, evento.getType());
        assertEquals("Ya no lo necesito", evento.getDetail());
    }

    @Test
    void despacharUnaRecogidaProgramadaLaPoneEnCamino() {
        Pickup pickup = programada();

        service.ejecutar(comando(RecogidaComando.Tipo.DESPACHAR));

        assertEquals(PickupStatus.ON_THE_WAY, pickup.getStatus());
        verify(repository).save(pickup);
        PickupEvent evento = eventoGuardado();
        assertEquals(PickupEventType.DISPATCHED, evento.getType());
        assertEquals(PickupStatus.SCHEDULED, evento.getPreviousStatus());
    }

    @Test
    void despacharUnaRecogidaSinAsignarSeRechaza() {
        Pickup pickup = solicitada();

        service.ejecutar(comando(RecogidaComando.Tipo.DESPACHAR));

        assertEquals(PickupStatus.REQUESTED, pickup.getStatus());
        assertEquals(PickupEventType.REJECTED, eventoGuardado().getType());
        verify(repository, never()).save(any());
    }

    @Test
    void completarUnaRecogidaQueNoHaSalidoSeRechaza() {
        programada();

        service.ejecutar(comando(RecogidaComando.Tipo.COMPLETAR));

        assertEquals(PickupEventType.REJECTED, eventoGuardado().getType());
        verifyNoInteractions(publisher);
    }

    @Test
    void vincularOtroEnvioAUnaRecogidaYaVinculadaSeRechaza() {
        Pickup pickup = solicitada();
        pickup.vincularEnvio(GUIA, NOW);
        RecogidaComando vincular = new RecogidaComando("event-1", RecogidaComando.Tipo.VINCULAR, NUMBER, "operador",
                NOW, null, null, null, null, null, null, null, null, "TF0123456789AC", null, null);

        service.ejecutar(vincular);

        assertEquals(GUIA, pickup.getShipmentTrackingNumber());
        assertEquals(PickupEventType.REJECTED, eventoGuardado().getType());
    }

    @Test
    void vincularUnEnvioYaUsadoEnOtraRecogidaSeRechaza() {
        solicitada();
        when(repository.existsActiveByShipmentExcept(GUIA, NUMBER)).thenReturn(true);
        RecogidaComando vincular = new RecogidaComando("event-1", RecogidaComando.Tipo.VINCULAR, NUMBER, "operador",
                NOW, null, null, null, null, null, null, null, null, GUIA, null, null);

        service.ejecutar(vincular);

        PickupEvent evento = eventoGuardado();
        assertEquals(PickupEventType.REJECTED, evento.getType());
        assertTrue(evento.getDetail().contains(new EnvioYaVinculadoException(GUIA).getMessage()));
    }

    @Test
    void unaReentregaDetectadaDespuesDeBloquearLaRecogidaSeDescarta() {
        for (RecogidaComando.Tipo tipo : new RecogidaComando.Tipo[] {
                RecogidaComando.Tipo.REPROGRAMAR, RecogidaComando.Tipo.CANCELAR, RecogidaComando.Tipo.DESPACHAR,
                RecogidaComando.Tipo.COMPLETAR, RecogidaComando.Tipo.VINCULAR }) {
            Pickup pickup = programada();
            // La primera consulta ve el comando como nuevo; la segunda, ya con la
            // recogida bloqueada, descubre que otro consumidor lo aplicó.
            when(events.existsByEventId("event-1")).thenReturn(false, true);

            service.ejecutar(reprogramacionDeTipo(tipo));

            assertEquals(PickupStatus.SCHEDULED, pickup.getStatus());
        }
        verify(repository, never()).save(any());
        verify(events, never()).save(any());
    }

    private RecogidaComando reprogramacionDeTipo(RecogidaComando.Tipo tipo) {
        return new RecogidaComando("event-1", tipo, NUMBER, "operador", NOW,
                null, null, null, null, FECHA.plusDays(1), FRANJA, null, null, GUIA, null, null);
    }
}
