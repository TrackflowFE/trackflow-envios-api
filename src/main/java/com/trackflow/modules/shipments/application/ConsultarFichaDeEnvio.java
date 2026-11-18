package com.trackflow.modules.shipments.application;

import com.trackflow.modules.shipments.domain.NumeroDeSeguimientoInvalidoException;
import com.trackflow.modules.shipments.domain.Shipment;
import com.trackflow.modules.shipments.domain.ShipmentNotFoundException;
import com.trackflow.modules.shipments.domain.TrackingNumber;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ficha del envío para atención y operación (HU-13).
 *
 * La versión completa no es "la misma consulta con un parámetro más": es otra
 * operación, con otro permiso y con registro de quién la usó, porque ver los datos
 * personales de alguien es un acto que debe poder auditarse.
 */
@Service
public class ConsultarFichaDeEnvio {

    private final ShipmentRepository shipments;
    private final CatalogoDeCiudades ciudades;
    private final RegistroDeAccesos accesos;

    public ConsultarFichaDeEnvio(ShipmentRepository shipments, CatalogoDeCiudades ciudades,
            RegistroDeAccesos accesos) {
        this.shipments = shipments;
        this.ciudades = ciudades;
        this.accesos = accesos;
    }

    @Transactional(readOnly = true)
    public FichaDeEnvio enmascarada(String trackingNumber) {
        return FichaDeEnvio.enmascarada(buscar(trackingNumber), ciudades);
    }

    @Transactional
    public FichaDeEnvio completa(String actor, String trackingNumber) {
        Shipment shipment = buscar(trackingNumber);
        // Se registra antes de devolver: si algo falla después, el acceso ya ocurrió.
        accesos.accesoADatosCompletos(actor, shipment.getTrackingNumber().value());
        return FichaDeEnvio.completa(shipment, ciudades);
    }

    private Shipment buscar(String trackingNumber) {
        TrackingNumber numero;
        try {
            numero = TrackingNumber.of(trackingNumber);
        } catch (IllegalArgumentException e) {
            throw new NumeroDeSeguimientoInvalidoException(trackingNumber);
        }
        return shipments.findByTrackingNumber(numero)
                .orElseThrow(() -> new ShipmentNotFoundException(trackingNumber));
    }
}
