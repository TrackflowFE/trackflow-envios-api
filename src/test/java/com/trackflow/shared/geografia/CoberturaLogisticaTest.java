package com.trackflow.shared.geografia;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** Las exigencias de cobertura y ruta que usan los casos de uso. */
class CoberturaLogisticaTest {

    private static final Ciudad MEDELLIN = new Ciudad(1L, "MEDELLÍN", "ANTIOQUIA");
    private static final Ciudad BOGOTA = new Ciudad(10L, "BOGOTÁ D.C.", "BOGOTÁ");
    private static final Ciudad LETICIA = new Ciudad(20L, "LETICIA", "AMAZONAS");

    /** Red mínima: Medellín y Bogotá tienen centros y una ruta de ida; Leticia no tiene centros. */
    private final CoberturaLogistica cobertura = new CoberturaLogistica() {
        @Override
        public boolean tieneCobertura(Long ciudadId) {
            return ciudadesConCobertura().contains(ciudadId);
        }

        @Override
        public Set<Long> ciudadesConCobertura() {
            return Set.of(1L, 10L);
        }

        @Override
        public boolean hayRuta(Long origenId, Long destinoId) {
            return origenId == 1L && destinoId == 10L;
        }
    };

    @Test
    void exigirNoFallaSiLaCiudadTieneCobertura() {
        assertDoesNotThrow(() -> cobertura.exigir(MEDELLIN));
    }

    @Test
    void exigirFallaSiLaCiudadNoTieneCentrosActivos() {
        CiudadSinCoberturaException e = assertThrows(CiudadSinCoberturaException.class,
                () -> cobertura.exigir(LETICIA));

        assertEquals(20L, e.getId());
        assertTrue(e.getMessage().startsWith("No hay cobertura en LETICIA - AMAZONAS"));
        assertTrue(e.getMessage().contains("onlyCovered=true"));
    }

    @Test
    void exigirRutaNoFallaSiLaRedConectaLasCiudades() {
        assertDoesNotThrow(() -> cobertura.exigirRuta(MEDELLIN, BOGOTA));
    }

    @Test
    void exigirRutaFallaSiLaRedNoLasConecta() {
        SinRutaEntreCiudadesException e = assertThrows(SinRutaEntreCiudadesException.class,
                () -> cobertura.exigirRuta(BOGOTA, MEDELLIN));

        assertEquals("La red no tiene una ruta de BOGOTÁ D.C. - BOGOTÁ a MEDELLÍN - ANTIOQUIA, "
                + "así que el envío no podría salir del origen", e.getMessage());
    }
}
