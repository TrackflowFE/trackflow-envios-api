package com.trackflow.modules.pickups.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trackflow.modules.pickups.domain.CodigoDeGestionInvalidoException;
import com.trackflow.modules.pickups.domain.HorarioDeRecogida;
import com.trackflow.modules.pickups.domain.PackageSize;
import com.trackflow.modules.pickups.domain.Pickup;
import com.trackflow.modules.pickups.domain.PickupNumber;
import com.trackflow.modules.pickups.domain.RecogidaNoEncontradaException;
import com.trackflow.modules.pickups.domain.ReprogramacionSinCambiosException;
import com.trackflow.modules.pickups.domain.TransicionDeRecogidaInvalidaException;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AdmitirRecogidaTransicionesTest {

    private static final String NUMBER = "RC0123456789AB";
    private static final String GUIA = "TF0123456789AB";
    private static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 17);
    private static final String FRANJA = "08:00-10:00";

    private final PickupRepository recogidas = mock(PickupRepository.class);
    private final RecogidaComandoPublisher publisher = mock(RecogidaComandoPublisher.class);
    private final HorarioDeRecogida horario = mock(HorarioDeRecogida.class);
    private final OperadoresDisponibles operadores = mock(OperadoresDisponibles.class);
    private final EnviosRegistrados envios = mock(EnviosRegistrados.class);
    private final CodigosDeGestion codigos = mock(CodigosDeGestion.class);
    private final PickupNumberGenerator numeros = mock(PickupNumberGenerator.class);
    private final AdmitirRecogida admitir = new AdmitirRecogida(numeros, publisher, mock(CatalogoDeCiudades.class),
            horario, operadores, envios, recogidas, Clock.fixed(NOW, ZoneOffset.UTC), codigos);

    private Pickup solicitada() {
        return Pickup.registrar(PickupNumber.of(NUMBER), "Calle 1", 1L, "Ana", "3001234567", FECHA, FRANJA,
                PackageSize.MEDIUM, BigDecimal.ONE, null, null, "operador", NOW, "hash");
    }

    private Pickup programada() {
        Pickup pickup = solicitada();
        pickup.reprogramar(FECHA, FRANJA, "operador", NOW);
        return pickup;
    }

    private Pickup enCamino() {
        Pickup pickup = programada();
        pickup.despachar(NOW);
        return pickup;
    }

    private Pickup recogida() {
        Pickup pickup = enCamino();
        pickup.completar(NOW);
        return pickup;
    }

    private Pickup cancelada() {
        Pickup pickup = solicitada();
        pickup.cancelar(NOW);
        return pickup;
    }

    private void existe(Pickup pickup) {
        when(recogidas.findByNumber(NUMBER)).thenReturn(Optional.of(pickup));
    }

    private RecogidaComando publicado() {
        ArgumentCaptor<RecogidaComando> captor = ArgumentCaptor.forClass(RecogidaComando.class);
        verify(publisher).publicar(captor.capture());
        return captor.getValue();
    }

    @Test
    void crearConOperadorYGuiaLosValidaAntesDePublicar() {
        when(numeros.next()).thenReturn(PickupNumber.of(NUMBER));
        when(codigos.generar()).thenReturn("ABCDEFGH");

        admitir.crear(new AdmitirRecogida.Solicitud("Calle 1", 1L, "Ana", "3001234567", FECHA, FRANJA,
                PackageSize.MEDIUM, BigDecimal.ONE, GUIA, "operador", "supervisor"));

        verify(operadores).exigir("operador");
        verify(envios).exigir(GUIA, 1L);
        assertEquals(RecogidaComando.Tipo.CREAR, publicado().comando());
    }

    @Test
    void reprogramarUnaRecogidaQueNoExisteSeRechaza() {
        when(recogidas.findByNumber(NUMBER)).thenReturn(Optional.empty());

        assertThrows(RecogidaNoEncontradaException.class,
                () -> admitir.reprogramar(NUMBER, FECHA, FRANJA, null, "operador"));
        verifyNoInteractions(publisher);
    }

    @Test
    void reprogramarUnaRecogidaEnCaminoSeRechaza() {
        existe(enCamino());

        assertThrows(TransicionDeRecogidaInvalidaException.class,
                () -> admitir.reprogramar(NUMBER, FECHA.plusDays(1), FRANJA, null, "operador"));
        verifyNoInteractions(publisher);
    }

    @Test
    void reprogramarSinCambiarNadaSeRechaza() {
        existe(programada());

        assertThrows(ReprogramacionSinCambiosException.class,
                () -> admitir.reprogramar(NUMBER, FECHA, FRANJA, null, "operador"));
        assertThrows(ReprogramacionSinCambiosException.class,
                () -> admitir.reprogramar(NUMBER, FECHA, FRANJA, "operador", "operador"));
        verifyNoInteractions(publisher);
    }

    @Test
    void asignarOtroOperadorSinCambiarLaFechaSePublica() {
        existe(programada());

        admitir.reprogramar(NUMBER, FECHA, FRANJA, "otro-operador", "supervisor");

        verify(operadores).exigir("otro-operador");
        RecogidaComando comando = publicado();
        assertEquals(RecogidaComando.Tipo.REPROGRAMAR, comando.comando());
        assertEquals("otro-operador", comando.assignedUsername());
        assertEquals("supervisor", comando.actor());
    }

    @Test
    void cambiarSoloLaFranjaNoExigeOperador() {
        existe(solicitada());

        admitir.reprogramar(NUMBER, FECHA, "10:00-12:00", " ", "operador");

        verify(horario).exigir(FECHA, "10:00-12:00", NOW);
        verifyNoInteractions(operadores);
        assertEquals("10:00-12:00", publicado().timeSlot());
    }

    @Test
    void cancelarPublicaElMotivo() {
        existe(programada());

        admitir.cancelar(NUMBER, "Ya no lo necesito", "operador");

        RecogidaComando comando = publicado();
        assertEquals(RecogidaComando.Tipo.CANCELAR, comando.comando());
        assertEquals("Ya no lo necesito", comando.reason());
        assertEquals(NOW, comando.occurredAt());
    }

    @Test
    void cancelarUnaRecogidaEnCaminoSeRechaza() {
        existe(enCamino());

        assertThrows(TransicionDeRecogidaInvalidaException.class,
                () -> admitir.cancelar(NUMBER, "motivo", "operador"));
        verifyNoInteractions(publisher);
    }

    @Test
    void despacharUnaRecogidaProgramadaSePublica() {
        existe(programada());

        admitir.despachar(NUMBER, "operador");

        RecogidaComando comando = publicado();
        assertEquals(RecogidaComando.Tipo.DESPACHAR, comando.comando());
        assertEquals(NUMBER, comando.pickupNumber());
        assertNull(comando.reason());
    }

    @Test
    void despacharUnaRecogidaSinAsignarSeRechaza() {
        existe(solicitada());

        assertThrows(TransicionDeRecogidaInvalidaException.class, () -> admitir.despachar(NUMBER, "operador"));
        verifyNoInteractions(publisher);
    }

    @Test
    void completarUnaRecogidaEnCaminoSePublica() {
        existe(enCamino());

        admitir.completar(NUMBER, "operador");

        assertEquals(RecogidaComando.Tipo.COMPLETAR, publicado().comando());
    }

    @Test
    void completarUnaRecogidaQueNoHaSalidoSeRechaza() {
        existe(programada());

        assertThrows(TransicionDeRecogidaInvalidaException.class, () -> admitir.completar(NUMBER, "operador"));
        verifyNoInteractions(publisher);
    }

    @Test
    void vincularUnEnvioValidoSePublica() {
        existe(recogida());

        admitir.vincular(NUMBER, GUIA, "operador");

        verify(envios).exigir(GUIA, 1L);
        RecogidaComando comando = publicado();
        assertEquals(RecogidaComando.Tipo.VINCULAR, comando.comando());
        assertEquals(GUIA, comando.shipmentTrackingNumber());
    }

    @Test
    void vincularUnaRecogidaCanceladaSeRechaza() {
        existe(cancelada());

        assertThrows(TransicionDeRecogidaInvalidaException.class, () -> admitir.vincular(NUMBER, GUIA, "operador"));
        verifyNoInteractions(publisher, envios);
    }

    @Test
    void vincularOtroEnvioAUnaRecogidaYaVinculadaSeRechaza() {
        Pickup pickup = solicitada();
        pickup.vincularEnvio(GUIA, NOW);
        existe(pickup);

        assertThrows(IllegalStateException.class, () -> admitir.vincular(NUMBER, "TF0123456789AC", "operador"));
        verifyNoInteractions(publisher, envios);
    }

    @Test
    void vincularDeNuevoElMismoEnvioSePermite() {
        Pickup pickup = solicitada();
        pickup.vincularEnvio(GUIA, NOW);
        existe(pickup);

        admitir.vincular(NUMBER, GUIA, "operador");

        assertEquals(GUIA, publicado().shipmentTrackingNumber());
    }

    @Test
    void elRemitenteConCodigoValidoCancela() {
        existe(solicitada());
        when(codigos.coincide("ABCDEFGH", "hash")).thenReturn(true);

        admitir.cancelarComoRemitente(NUMBER, "ABCDEFGH", "Cambié de planes");

        RecogidaComando comando = publicado();
        assertEquals(RecogidaComando.Tipo.CANCELAR, comando.comando());
        assertEquals("remitente", comando.actor());
    }

    @Test
    void elRemitenteNoGestionaUnaRecogidaSinCodigo() {
        existe(Pickup.registrar(PickupNumber.of(NUMBER), "Calle 1", 1L, "Ana", "3001234567", FECHA, FRANJA,
                PackageSize.MEDIUM, BigDecimal.ONE, null, null, "operador", NOW));
        when(codigos.coincide("ABCDEFGH", null)).thenReturn(true);

        assertThrows(CodigoDeGestionInvalidoException.class,
                () -> admitir.cancelarComoRemitente(NUMBER, "ABCDEFGH", "motivo"));
        verifyNoInteractions(publisher);
    }

    @Test
    void elRemitenteConCodigoIncorrectoNoCancela() {
        existe(solicitada());
        when(codigos.coincide("ZZZZZZZZ", "hash")).thenReturn(false);

        assertThrows(CodigoDeGestionInvalidoException.class,
                () -> admitir.cancelarComoRemitente(NUMBER, "ZZZZZZZZ", "motivo"));
        verifyNoInteractions(publisher);
    }
}
