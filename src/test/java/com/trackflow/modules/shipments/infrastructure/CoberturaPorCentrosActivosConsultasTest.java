package com.trackflow.modules.shipments.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

/**
 * Las consultas de cobertura y de ruta contra la base, con JdbcTemplate simulado. La regla
 * de alcance en sí está en CoberturaPorCentrosActivosTest.
 */
class CoberturaPorCentrosActivosConsultasTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final CoberturaPorCentrosActivos cobertura = new CoberturaPorCentrosActivos(jdbc);

    @Test
    void unaCiudadConCentroActivoTieneCobertura() {
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(1L))).thenReturn(true);

        assertTrue(cobertura.tieneCobertura(1L));
    }

    @Test
    void unaCiudadSinCentroActivoNoTieneCobertura() {
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(2L))).thenReturn(false);

        assertFalse(cobertura.tieneCobertura(2L));
    }

    @Test
    void unResultadoNuloSeTomaComoSinCobertura() {
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(3L))).thenReturn(null);

        assertFalse(cobertura.tieneCobertura(3L));
    }

    @Test
    void sinCiudadNoHayCoberturaNiSeConsultaLaBase() {
        assertFalse(cobertura.tieneCobertura(null));
        verifyNoInteractions(jdbc);
    }

    @Test
    void devuelveLasCiudadesConCoberturaSinRepetir() {
        when(jdbc.queryForList(anyString(), eq(Long.class))).thenReturn(List.of(1L, 10L, 1L));

        assertEquals(Set.of(1L, 10L), cobertura.ciudadesConCobertura());
    }

    @Test
    void hayRutaSiLosTramosActivosConectanLasCiudades() {
        conTramos(new long[][] { { 1L, 2L }, { 2L, 3L } });

        assertTrue(cobertura.hayRuta(1L, 3L));
    }

    @Test
    void noHayRutaSiLosTramosNoLleganAlDestino() {
        conTramos(new long[][] { { 1L, 2L } });

        assertFalse(cobertura.hayRuta(1L, 3L));
    }

    @Test
    void sinOrigenOSinDestinoNoHayRutaNiSeConsultaLaBase() {
        assertFalse(cobertura.hayRuta(null, 3L));
        assertFalse(cobertura.hayRuta(1L, null));
        verifyNoInteractions(jdbc);
    }

    private void conTramos(long[][] tramos) {
        doAnswer(invocacion -> {
            RowCallbackHandler handler = invocacion.getArgument(1);
            for (long[] tramo : tramos) {
                ResultSet fila = mock(ResultSet.class);
                when(fila.getLong("from_city_id")).thenReturn(tramo[0]);
                when(fila.getLong("to_city_id")).thenReturn(tramo[1]);
                handler.processRow(fila);
            }
            return null;
        }).when(jdbc).query(anyString(), any(RowCallbackHandler.class));
    }
}
