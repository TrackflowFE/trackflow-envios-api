package com.trackflow.modules.pickups.application;

import com.trackflow.modules.pickups.domain.Pickup;
import com.trackflow.modules.pickups.domain.PickupEvent;
import com.trackflow.modules.pickups.domain.PickupStatus;
import com.trackflow.modules.pickups.domain.RecogidaNoEncontradaException;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import com.trackflow.shared.geografia.Ciudad;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Consulta de las recogidas.
 *
 * Aquí se decide qué ve cada quien: la dirección y el contacto solo se devuelven al
 * operador que tiene la recogida asignada. Quien no la tiene recibe el resto de los
 * datos y una marca de que los datos privados no están disponibles, en vez de un
 * error: la operación sigue siendo válida, lo que no se puede ver es el domicilio.
 */
@Service
public class ConsultarRecogidas {

    private final PickupRepository recogidas;
    private final PickupEventRepository eventos;
    private final CatalogoDeCiudades ciudades;

    public ConsultarRecogidas(PickupRepository recogidas, PickupEventRepository eventos, CatalogoDeCiudades ciudades) {
        this.recogidas = recogidas;
        this.eventos = eventos;
        this.ciudades = ciudades;
    }

    /**
     * @param solicitante el usuario que consulta, o null si no hay sesión abierta
     */
    public VistaDeRecogida una(String pickupNumber, String solicitante) {
        Pickup pickup = recogidas.findByNumber(pickupNumber)
                .orElseThrow(() -> new RecogidaNoEncontradaException(pickupNumber));

        return vista(pickup, solicitante);
    }

    public List<VistaDeRecogida> listar(PickupStatus status, String solicitante, boolean soloAsignadas) {
        return recogidas.search(status).stream()
                .filter(pickup -> !soloAsignadas || (solicitante != null
                        && solicitante.equals(pickup.getAssignedUsername())))
                .map(pickup -> vista(pickup, solicitante))
                .toList();
    }

    private VistaDeRecogida vista(Pickup pickup, String solicitante) {
        String ciudad = ciudades.porId(pickup.getCityId()).map(Ciudad::etiqueta).orElse(null);
        List<PickupEvent> historial = eventos.findByPickupNumber(pickup.getPickupNumber().value());

        return new VistaDeRecogida(pickup, ciudad, historial, pickup.puedeVerDatosPrivados(solicitante));
    }
}
