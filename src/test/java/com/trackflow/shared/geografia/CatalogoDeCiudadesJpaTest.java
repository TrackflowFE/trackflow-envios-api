package com.trackflow.shared.geografia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Limit;
import org.springframework.test.util.ReflectionTestUtils;

class CatalogoDeCiudadesJpaTest {

    private final CiudadJpaRepository jpa = mock(CiudadJpaRepository.class);
    private final CatalogoDeCiudadesJpa catalogo = new CatalogoDeCiudadesJpa(jpa);

    private static CiudadRegistrada registrada(Long id, String nombre, String departamento) {
        CiudadRegistrada ciudad = new CiudadRegistrada();
        ReflectionTestUtils.setField(ciudad, "id", id);
        ReflectionTestUtils.setField(ciudad, "name", nombre);
        ReflectionTestUtils.setField(ciudad, "department", departamento);
        return ciudad;
    }

    @Test
    void buscaPorIdYConvierteAlModeloDelCatalogo() {
        when(jpa.findById(1L)).thenReturn(Optional.of(registrada(1L, "MEDELLÍN", "ANTIOQUIA")));

        assertEquals(Optional.of(new Ciudad(1L, "MEDELLÍN", "ANTIOQUIA")), catalogo.porId(1L));
    }

    @Test
    void unIdInexistenteDevuelveVacio() {
        when(jpa.findById(99L)).thenReturn(Optional.empty());

        assertTrue(catalogo.porId(99L).isEmpty());
    }

    @Test
    void unIdNuloDevuelveVacioSinConsultar() {
        assertTrue(catalogo.porId(null).isEmpty());
        verifyNoInteractions(jpa);
    }

    @Test
    void buscaConElTextoSinEspaciosYElLimitePedido() {
        when(jpa.buscar("bogo", Limit.of(5))).thenReturn(List.of(registrada(10L, "BOGOTÁ D.C.", "BOGOTÁ")));

        List<Ciudad> encontradas = catalogo.buscar("  bogo ", 5);

        assertEquals(List.of(new Ciudad(10L, "BOGOTÁ D.C.", "BOGOTÁ")), encontradas);
        verify(jpa).buscar("bogo", Limit.of(5));
    }

    @Test
    void unTextoVacioONuloNoBuscaNada() {
        assertTrue(catalogo.buscar(null, 5).isEmpty());
        assertTrue(catalogo.buscar("   ", 5).isEmpty());
        verifyNoInteractions(jpa);
    }
}
