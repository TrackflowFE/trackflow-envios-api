package com.trackflow.modules.shipments.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.trackflow.modules.shipments.domain.NumeroDeSeguimientoInvalidoException;
import com.trackflow.modules.shipments.domain.Party;
import com.trackflow.modules.shipments.domain.Shipment;
import com.trackflow.modules.shipments.domain.ShipmentNotFoundException;
import com.trackflow.modules.shipments.domain.TipoDocumento;
import com.trackflow.modules.shipments.domain.TrackingNumber;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import com.trackflow.shared.geografia.Ciudad;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** HU-14: la reimpresión busca por número y responde 404 o 400 según el caso. */
class ConsultarComprobanteTest {

    private static final String TN = "TF0123456789AB";
    private static final Ciudad MEDELLIN = new Ciudad(1L, "MEDELLÍN", "ANTIOQUIA");
    private static final Ciudad BOGOTA = new Ciudad(10L, "BOGOTÁ D.C.", "BOGOTÁ");

    private final ShipmentRepository shipments = mock(ShipmentRepository.class);
    private final CatalogoDeCiudades ciudades = mock(CatalogoDeCiudades.class);
    private final ConsultarComprobante consultar = new ConsultarComprobante(shipments, ciudades);

    private Shipment envio() {
        return Shipment.registrar(TrackingNumber.of(TN),
                new Party("Ana Torres", TipoDocumento.CC, "1000000001", "3000000001", "Calle 1", 1L),
                new Party("Luis Gómez", TipoDocumento.CC, "2000000002", "3000000002", "Carrera 2", 10L),
                "Documentos", Instant.parse("2026-11-16T15:04:05Z"));
    }

    @Test
    void devuelveElComprobanteDeUnEnvioExistente() {
        when(shipments.findByTrackingNumber(TrackingNumber.of(TN))).thenReturn(Optional.of(envio()));
        when(ciudades.porId(1L)).thenReturn(Optional.of(MEDELLIN));
        when(ciudades.porId(10L)).thenReturn(Optional.of(BOGOTA));

        Comprobante comprobante = consultar.porNumero(TN);

        assertEquals(TN, comprobante.trackingNumber());
        assertEquals("Ana Torres", comprobante.senderName());
        assertEquals("BOGOTÁ D.C. - BOGOTÁ", comprobante.destinationCity());
    }

    @Test
    void unEnvioInexistenteResponde404() {
        when(shipments.findByTrackingNumber(TrackingNumber.of(TN))).thenReturn(Optional.empty());

        assertThrows(ShipmentNotFoundException.class, () -> consultar.porNumero(TN));
    }

    @Test
    void unFormatoInvalidoSeRechazaAntesDeConsultar() {
        assertThrows(NumeroDeSeguimientoInvalidoException.class, () -> consultar.porNumero("no-es-una-guia"));
    }
}
