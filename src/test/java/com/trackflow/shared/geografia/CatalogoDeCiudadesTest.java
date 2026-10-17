package com.trackflow.shared.geografia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** La forma en que los casos de uso exigen una ciudad del catálogo. */
class CatalogoDeCiudadesTest {

    private static final Ciudad MEDELLIN = new Ciudad(1L, "MEDELLÍN", "ANTIOQUIA");

    private final CatalogoDeCiudades catalogo = new CatalogoDeCiudades() {
        private final Map<Long, Ciudad> ciudades = Map.of(1L, MEDELLIN);

        @Override
        public Optional<Ciudad> porId(Long id) {
            return Optional.ofNullable(id == null ? null : ciudades.get(id));
        }

        @Override
        public List<Ciudad> buscar(String texto, int limite) {
            return List.of();
        }
    };

    @Test
    void exigirDevuelveLaCiudadSiEstaEnElCatalogo() {
        assertEquals(MEDELLIN, catalogo.exigir(1L));
    }

    @Test
    void exigirFallaConElIdYLaPistaDeDondeConsultarlos() {
        CiudadDesconocidaException e = assertThrows(CiudadDesconocidaException.class, () -> catalogo.exigir(99L));

        assertEquals(99L, e.getId());
        assertTrue(e.getMessage().startsWith("La ciudad 99 no está en el catálogo"));
        assertTrue(e.getMessage().contains("GET /api/cities?q="));
    }

    @Test
    void laEtiquetaUneNombreYDepartamento() {
        assertEquals("MEDELLÍN - ANTIOQUIA", MEDELLIN.etiqueta());
    }
}
