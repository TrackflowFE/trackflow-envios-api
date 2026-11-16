package com.trackflow.modules.pickups.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.trackflow.modules.pickups.domain.*;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AutogestionRecogidasTest {
    private static final String NUMBER = "RC0123456789AB";
    private static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");
    private final PickupRepository recogidas = mock(PickupRepository.class);
    private final PickupEventRepository eventos = mock(PickupEventRepository.class);
    private final CatalogoDeCiudades ciudades = mock(CatalogoDeCiudades.class);
    private final CodigosDeGestion codigos = mock(CodigosDeGestion.class);
    private final RecogidaComandoPublisher publisher = mock(RecogidaComandoPublisher.class);
    private final EnviosRegistrados envios = mock(EnviosRegistrados.class);
    private final PickupNumberGenerator numeros = mock(PickupNumberGenerator.class);
    private final AdmitirRecogida admitir = new AdmitirRecogida(numeros, publisher, ciudades,
            mock(HorarioDeRecogida.class), mock(OperadoresDisponibles.class), envios, recogidas,
            Clock.fixed(NOW, ZoneOffset.UTC), codigos);
    private final ConsultarRecogidas consultar = new ConsultarRecogidas(recogidas, eventos, ciudades, codigos);

    private Pickup pickup() {
        return Pickup.registrar(PickupNumber.of(NUMBER), "Dirección privada", 1L, "Ana", "3001234567",
                LocalDate.of(2026, 11, 17), "08:00-10:00", PackageSize.MEDIUM, BigDecimal.ONE,
                null, null, "remitente", NOW, "hash");
    }

    @Test
    void remitenteAutenticadoVePrivadosYReprogramaSinAsignar() {
        Pickup pickup = pickup();
        pickup.reprogramar(pickup.getScheduledDate(), pickup.getTimeSlot(), "operador", NOW);
        when(recogidas.findByNumber(NUMBER)).thenReturn(Optional.of(pickup));
        when(codigos.coincide("ABCDEFGH", "hash")).thenReturn(true);
        assertTrue(consultar.delRemitente(NUMBER, "ABCDEFGH").datosPrivadosVisibles());
        assertFalse(consultar.una(NUMBER, "otro").datosPrivadosVisibles());
        assertTrue(consultar.una(NUMBER, "operador").datosPrivadosVisibles());
        admitir.reprogramarComoRemitente(NUMBER, "ABCDEFGH", LocalDate.of(2026, 11, 18), "10:00-12:00");
        ArgumentCaptor<RecogidaComando> comando = ArgumentCaptor.forClass(RecogidaComando.class);
        verify(publisher).publicar(comando.capture());
        assertEquals("remitente", comando.getValue().actor());
        assertNull(comando.getValue().assignedUsername());
    }

    @Test
    void codigoIncorrectoYNoExistenteCompartenErrorYCompruebanHash() {
        when(recogidas.findByNumber(NUMBER)).thenReturn(Optional.of(pickup()));
        assertEquals(assertThrows(CodigoDeGestionInvalidoException.class,
                () -> consultar.delRemitente(NUMBER, "MAL")).getMessage(),
                assertThrows(CodigoDeGestionInvalidoException.class,
                        () -> consultar.delRemitente("RC0123456789AC", "MAL")).getMessage());
        verify(codigos).coincide("MAL", "hash");
        verify(codigos).coincide("MAL", null);
    }

    @Test
    void envioAvanzadoYDuplicadoSeRechazan() {
        EnviosRegistrados registrados = tracking -> Optional.of(new EnviosRegistrados.EnvioRegistrado(tracking, 1L, "IN_TRANSIT"));
        assertThrows(EnvioYaEnRecorridoException.class, () -> registrados.exigir("TF0123456789AB", 1L));
        when(recogidas.findByNumber(NUMBER)).thenReturn(Optional.of(pickup()));
        when(recogidas.existsActiveByShipmentExcept("TF0123456789AB", NUMBER)).thenReturn(true);
        assertThrows(EnvioYaVinculadoException.class, () -> admitir.vincular(NUMBER, "TF0123456789AB", "operador"));
        verifyNoInteractions(publisher);
    }

    private AdmitirRecogida.Solicitud solicitud(String guia) {
        return new AdmitirRecogida.Solicitud("Calle 1", 1L, "Ana", "3001234567",
                LocalDate.of(2026, 11, 17), "08:00-10:00", PackageSize.MEDIUM,
                BigDecimal.ONE, guia, null, "operador");
    }

    @Test
    void rechazaGuiaAvanzadaODuplicadaAlCrearYVincular() {
        String guia = "TF0123456789AB";
        when(recogidas.findByNumber(NUMBER)).thenReturn(Optional.of(pickup()));
        doThrow(new EnvioYaEnRecorridoException(guia)).when(envios).exigir(guia, 1L);
        assertThrows(EnvioYaEnRecorridoException.class, () -> admitir.crear(solicitud(guia)));
        assertThrows(EnvioYaEnRecorridoException.class, () -> admitir.vincular(NUMBER, guia, "operador"));
        doNothing().when(envios).exigir(guia, 1L);
        when(recogidas.existsActiveByShipmentExcept(guia, null)).thenReturn(true);
        when(recogidas.existsActiveByShipmentExcept(guia, NUMBER)).thenReturn(true);
        assertThrows(EnvioYaVinculadoException.class, () -> admitir.crear(solicitud(guia)));
        assertThrows(EnvioYaVinculadoException.class, () -> admitir.vincular(NUMBER, guia, "operador"));
        verifyNoInteractions(publisher);
    }

    @Test
    void creacionDevuelveCodigoPeroPublicaSoloHash() {
        when(numeros.next()).thenReturn(PickupNumber.of(NUMBER));
        when(codigos.generar()).thenReturn("ABCDEFGH");
        when(codigos.hash("ABCDEFGH")).thenReturn("$2a$hash");
        RecogidaSolicitada resultado = admitir.crear(solicitud(null));
        ArgumentCaptor<RecogidaComando> captor = ArgumentCaptor.forClass(RecogidaComando.class);
        verify(publisher).publicar(captor.capture());
        assertEquals("ABCDEFGH", resultado.managementCode());
        assertEquals("$2a$hash", captor.getValue().managementCodeHash());
        assertFalse(captor.getValue().toString().contains("ABCDEFGH"));
    }
}
