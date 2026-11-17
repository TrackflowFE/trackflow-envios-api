package com.trackflow.modules.shipments.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.trackflow.modules.shipments.domain.Party;
import com.trackflow.modules.shipments.domain.Shipment;
import com.trackflow.modules.shipments.domain.TipoDocumento;
import com.trackflow.modules.shipments.domain.TrackingNumber;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import com.trackflow.shared.geografia.Ciudad;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** HU-14: la impresión y la reimpresión salen del mismo comprobante y solo con lo permitido. */
class ComprobanteTest {

    private static final String TN = "TF0123456789AB";
    private static final Instant REGISTRADO = Instant.parse("2026-11-16T15:04:05.123456Z");
    private static final Ciudad MEDELLIN = new Ciudad(1L, "MEDELLÍN", "ANTIOQUIA");
    private static final Ciudad BOGOTA = new Ciudad(10L, "BOGOTÁ D.C.", "BOGOTÁ");

    private static final Party REMITENTE = new Party("Ana Torres", TipoDocumento.CC, "1000000001",
            "3000000001", "Calle 1 # 1-1", 1L);
    private static final Party DESTINATARIO = new Party("Luis Gómez", TipoDocumento.CC, "2000000002",
            "3000000002", "Carrera 2 # 2-2", 10L);

    private final CatalogoDeCiudades ciudades = mock(CatalogoDeCiudades.class);

    @Test
    void lasDosFabricasProducenElMismoComprobante() {
        when(ciudades.porId(1L)).thenReturn(Optional.of(MEDELLIN));
        when(ciudades.porId(10L)).thenReturn(Optional.of(BOGOTA));

        EnvioSolicitado solicitud = new EnvioSolicitado("evt-1", TN, REMITENTE, DESTINATARIO,
                MEDELLIN, BOGOTA, "Documentos", REGISTRADO);
        Shipment shipment = Shipment.registrar(TrackingNumber.of(TN), REMITENTE, DESTINATARIO,
                "Documentos", REGISTRADO);

        assertEquals(Comprobante.de(solicitud), Comprobante.de(shipment, ciudades));
        Comprobante comprobante = Comprobante.de(solicitud);
        assertEquals(TN, comprobante.trackingNumber());
        assertEquals(REGISTRADO, comprobante.registeredAt());
        assertEquals("Ana Torres", comprobante.senderName());
        assertEquals("MEDELLÍN - ANTIOQUIA", comprobante.originCity());
        assertEquals("Luis Gómez", comprobante.recipientName());
        assertEquals("BOGOTÁ D.C. - BOGOTÁ", comprobante.destinationCity());
    }

    @Test
    void noTieneCamposPrivados() {
        Set<String> campos = Arrays.stream(Comprobante.class.getRecordComponents())
                .map(componente -> componente.getName().toLowerCase())
                .collect(Collectors.toSet());

        List<String> prohibidos = List.of("phone", "telefono", "address", "direccion",
                "document", "documento", "description", "descripcion");
        for (String prohibido : prohibidos) {
            assertFalse(campos.stream().anyMatch(campo -> campo.contains(prohibido)),
                    "El comprobante no debe exponer " + prohibido + ": " + campos);
        }
    }

    @Test
    void unaCiudadFueraDelCatalogoEsUnDatoInconsistente() {
        when(ciudades.porId(1L)).thenReturn(Optional.of(MEDELLIN));
        when(ciudades.porId(10L)).thenReturn(Optional.empty());
        Shipment shipment = Shipment.registrar(TrackingNumber.of(TN), REMITENTE, DESTINATARIO,
                "Documentos", REGISTRADO);

        assertThrows(IllegalStateException.class, () -> Comprobante.de(shipment, ciudades));
    }
}
