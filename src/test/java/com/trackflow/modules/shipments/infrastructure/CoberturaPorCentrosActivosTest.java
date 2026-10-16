package com.trackflow.modules.shipments.infrastructure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** La regla de alcance entre ciudades, sin base de datos. */
class CoberturaPorCentrosActivosTest {

    @Test
    void unTramoDirectoConectaLasCiudades() {
        assertTrue(CoberturaPorCentrosActivos.alcanzable(Map.of(1L, List.of(2L)), 1L, 2L));
    }

    @Test
    void sePuedeLlegarPasandoPorOtrasCiudades() {
        Map<Long, List<Long>> red = Map.of(1L, List.of(2L), 2L, List.of(3L));

        assertTrue(CoberturaPorCentrosActivos.alcanzable(red, 1L, 3L));
    }

    @Test
    void sinTramosHastaElDestinoNoHayRuta() {
        assertFalse(CoberturaPorCentrosActivos.alcanzable(Map.of(1L, List.of(2L)), 1L, 3L));
    }

    @Test
    void losTramosSeRecorrenSoloEnSuSentido() {
        assertFalse(CoberturaPorCentrosActivos.alcanzable(Map.of(1L, List.of(2L)), 2L, 1L));
    }

    @Test
    void unCicloNoImpideConcluirQueNoHayRuta() {
        Map<Long, List<Long>> red = Map.of(1L, List.of(2L), 2L, List.of(1L));

        assertFalse(CoberturaPorCentrosActivos.alcanzable(red, 1L, 3L));
    }

    @Test
    void unaCiudadSiempreLlegaASiMisma() {
        assertTrue(CoberturaPorCentrosActivos.alcanzable(Map.of(), 5L, 5L));
    }
}
