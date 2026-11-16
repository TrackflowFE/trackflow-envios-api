package com.trackflow.modules.shipments.application;

import com.trackflow.modules.shipments.domain.NumeroDeSeguimientoInvalidoException;
import com.trackflow.modules.shipments.domain.Shipment;
import com.trackflow.modules.shipments.domain.ShipmentNotFoundException;
import com.trackflow.modules.shipments.domain.TrackingNumber;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reimpresión del comprobante (HU-14): busca el envío y lo devuelve en el mismo
 * formato que la admisión, para que el papel sea idéntico.
 */
@Service
public class ConsultarComprobante {

    private final ShipmentRepository shipments;
    private final CatalogoDeCiudades ciudades;

    public ConsultarComprobante(ShipmentRepository shipments, CatalogoDeCiudades ciudades) {
        this.shipments = shipments;
        this.ciudades = ciudades;
    }

    @Transactional(readOnly = true)
    public Comprobante porNumero(String trackingNumber) {
        TrackingNumber numero;
        try {
            numero = TrackingNumber.of(trackingNumber);
        } catch (IllegalArgumentException e) {
            throw new NumeroDeSeguimientoInvalidoException(trackingNumber);
        }
        Shipment shipment = shipments.findByTrackingNumber(numero)
                .orElseThrow(() -> new ShipmentNotFoundException(trackingNumber));

        return Comprobante.de(shipment, ciudades);
    }
}
