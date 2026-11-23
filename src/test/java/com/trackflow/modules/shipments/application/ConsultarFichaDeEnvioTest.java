package com.trackflow.modules.shipments.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trackflow.modules.shipments.domain.NumeroDeSeguimientoInvalidoException;
import com.trackflow.modules.shipments.domain.Party;
import com.trackflow.modules.shipments.domain.Shipment;
import com.trackflow.modules.shipments.domain.ShipmentNotFoundException;
import com.trackflow.modules.shipments.domain.TipoDocumento;
import com.trackflow.modules.shipments.domain.TrackingNumber;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConsultarFichaDeEnvioTest {

    private static final String NUMERO = "TF0123456789AB";

    private final ShipmentRepository shipments = mock(ShipmentRepository.class);
    private final CatalogoDeCiudades ciudades = mock(CatalogoDeCiudades.class);
    private final RegistroDeAccesos accesos = mock(RegistroDeAccesos.class);
    private final ConsultarFichaDeEnvio consulta = new ConsultarFichaDeEnvio(shipments, ciudades, accesos);

    private Shipment envio() {
        Party remitente = new Party("Ana", TipoDocumento.CC, "1000000001", "3001234567", "Calle 1", 1L);
        Party destinatario = new Party("Luis", TipoDocumento.CC, "2000000002", "3109876543", "Carrera 2", 10L);
        return Shipment.registrar(TrackingNumber.of(NUMERO), remitente, destinatario, "Documentos",
                Instant.parse("2026-10-15T18:00:00Z"));
    }

    private void existe() {
        when(shipments.findByTrackingNumber(TrackingNumber.of(NUMERO))).thenReturn(Optional.of(envio()));
    }

    @Test
    void laFichaEnmascaradaNoRegistraAccesoAlguno() {
        existe();

        FichaDeEnvio ficha = consulta.enmascarada(NUMERO);

        assertFalse(ficha.datosCompletos());
        assertEquals("********01", ficha.sender().documentNumber());
        verifyNoInteractions(accesos);
    }

    @Test
    void laFichaCompletaRegistraQuienLaConsulto() {
        existe();

        FichaDeEnvio ficha = consulta.completa("admin", NUMERO);

        assertTrue(ficha.datosCompletos());
        assertEquals("1000000001", ficha.sender().documentNumber());
        verify(accesos).accesoADatosCompletos("admin", NUMERO);
    }

    @Test
    void unNumeroConFormatoInvalidoSeRechazaSinConsultarNada() {
        assertThrows(NumeroDeSeguimientoInvalidoException.class, () -> consulta.enmascarada("no-es-un-numero"));

        verifyNoInteractions(shipments, accesos);
    }

    @Test
    void unEnvioInexistenteDaNoEncontrado() {
        when(shipments.findByTrackingNumber(TrackingNumber.of(NUMERO))).thenReturn(Optional.empty());

        assertThrows(ShipmentNotFoundException.class, () -> consulta.enmascarada(NUMERO));
    }

    @Test
    void siElEnvioNoExisteLaFichaCompletaNoRegistraAcceso() {
        when(shipments.findByTrackingNumber(TrackingNumber.of(NUMERO))).thenReturn(Optional.empty());

        assertThrows(ShipmentNotFoundException.class, () -> consulta.completa("admin", NUMERO));

        verifyNoInteractions(accesos);
    }
}
