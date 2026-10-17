package com.trackflow.modules.shipments.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trackflow.modules.shipments.domain.DocumentoInvalidoException;
import com.trackflow.modules.shipments.domain.TipoDocumento;
import com.trackflow.modules.shipments.domain.TrackingNumber;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import com.trackflow.shared.geografia.Ciudad;
import com.trackflow.shared.geografia.CiudadDesconocidaException;
import com.trackflow.shared.geografia.CiudadSinCoberturaException;
import com.trackflow.shared.geografia.CoberturaLogistica;
import com.trackflow.shared.geografia.SinRutaEntreCiudadesException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdmitirEnvioTest {

    private static final Instant AHORA = Instant.parse("2026-10-15T18:00:00Z");
    private static final Ciudad MEDELLIN = new Ciudad(1L, "MEDELLÍN", "ANTIOQUIA");
    private static final Ciudad BOGOTA = new Ciudad(10L, "BOGOTÁ D.C.", "BOGOTÁ");

    private final TrackingNumberGenerator trackingNumbers = mock(TrackingNumberGenerator.class);
    private final EnvioSolicitadoPublisher publisher = mock(EnvioSolicitadoPublisher.class);
    private final CatalogoDeCiudades ciudades = mock(CatalogoDeCiudades.class);
    private final CoberturaLogistica cobertura = mock(CoberturaLogistica.class);
    private final AdmitirEnvio admitir = new AdmitirEnvio(trackingNumbers, publisher, ciudades, cobertura,
            Clock.fixed(AHORA, ZoneOffset.UTC));

    private final DatosPersona remitente = new DatosPersona("Ana Torres", TipoDocumento.CC, "1000000001",
            "3000000001", "Calle 1 # 1-1", 1L);
    private final DatosPersona destinatario = new DatosPersona("Luis Gómez", TipoDocumento.CC, "2000000002",
            "3000000002", "Carrera 2 # 2-2", 10L);

    @BeforeEach
    void catalogo() {
        when(ciudades.exigir(any())).thenCallRealMethod();
        when(ciudades.porId(1L)).thenReturn(Optional.of(MEDELLIN));
        when(ciudades.porId(10L)).thenReturn(Optional.of(BOGOTA));
        when(trackingNumbers.next()).thenReturn(TrackingNumber.of("TF0123456789AB"));
    }

    private void conCoberturaYRuta() {
        when(cobertura.tieneCobertura(1L)).thenReturn(true);
        when(cobertura.tieneCobertura(10L)).thenReturn(true);
        when(cobertura.hayRuta(1L, 10L)).thenReturn(true);
        doCallRealMethod().when(cobertura).exigir(any());
        doCallRealMethod().when(cobertura).exigirRuta(any(), any());
    }

    @Test
    void admiteLaSolicitudYLaPublicaConElNumeroDeSeguimiento() {
        conCoberturaYRuta();

        EnvioSolicitado solicitud = admitir.ejecutar(new AdmitirEnvio.Command(remitente, destinatario, "Documentos"));

        assertEquals("TF0123456789AB", solicitud.trackingNumber());
        assertEquals(36, solicitud.eventId().length());
        assertSame(MEDELLIN, solicitud.ciudadOrigen());
        assertSame(BOGOTA, solicitud.ciudadDestino());
        assertEquals("Documentos", solicitud.descripcion());
        assertEquals(AHORA, solicitud.solicitadoEn());
        assertEquals("Ana Torres", solicitud.remitente().getFullName());
        assertEquals(1L, solicitud.remitente().getCityId());
        assertEquals("Luis Gómez", solicitud.destinatario().getFullName());
        assertEquals("Carrera 2 # 2-2", solicitud.destinatario().getAddress());
        assertEquals(10L, solicitud.destinatario().getCityId());
        verify(publisher).publicar(solicitud);
    }

    @Test
    void rechazaUnaCiudadQueNoEstaEnElCatalogo() {
        when(ciudades.porId(99L)).thenReturn(Optional.empty());
        DatosPersona sinCiudad = new DatosPersona("Luis", TipoDocumento.CC, "2000000002", "300", "Calle", 99L);

        CiudadDesconocidaException e = assertThrows(CiudadDesconocidaException.class,
                () -> admitir.ejecutar(new AdmitirEnvio.Command(remitente, sinCiudad, "Documentos")));

        assertEquals(99L, e.getId());
        verifyNoInteractions(publisher, trackingNumbers);
    }

    @Test
    void rechazaUnaCiudadSinCobertura() {
        conCoberturaYRuta();
        when(cobertura.tieneCobertura(10L)).thenReturn(false);

        CiudadSinCoberturaException e = assertThrows(CiudadSinCoberturaException.class,
                () -> admitir.ejecutar(new AdmitirEnvio.Command(remitente, destinatario, "Documentos")));

        assertEquals(10L, e.getId());
        verifyNoInteractions(publisher, trackingNumbers);
    }

    @Test
    void rechazaCiudadesQueLaRedNoConecta() {
        conCoberturaYRuta();
        when(cobertura.hayRuta(1L, 10L)).thenReturn(false);

        assertThrows(SinRutaEntreCiudadesException.class,
                () -> admitir.ejecutar(new AdmitirEnvio.Command(remitente, destinatario, "Documentos")));

        verifyNoInteractions(publisher, trackingNumbers);
    }

    @Test
    void rechazaUnDocumentoInvalidoSinPublicar() {
        conCoberturaYRuta();
        DatosPersona documentoMalo = new DatosPersona("Ana", TipoDocumento.CC, "ABC", "300", "Calle", 1L);

        assertThrows(DocumentoInvalidoException.class,
                () -> admitir.ejecutar(new AdmitirEnvio.Command(documentoMalo, destinatario, "Documentos")));

        verifyNoInteractions(publisher);
    }

    @Test
    void cadaSolicitudTieneSuPropioIdentificadorDeEvento() {
        conCoberturaYRuta();
        AdmitirEnvio.Command comando = new AdmitirEnvio.Command(remitente, destinatario, "Documentos");

        EnvioSolicitado primera = admitir.ejecutar(comando);
        EnvioSolicitado segunda = assertDoesNotThrow(() -> admitir.ejecutar(comando));

        assertNotEquals(primera.eventId(), segunda.eventId());
    }
}
