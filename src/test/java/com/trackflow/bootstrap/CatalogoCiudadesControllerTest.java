package com.trackflow.bootstrap;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackflow.shared.geografia.CatalogoDeCiudades;
import com.trackflow.shared.geografia.Ciudad;
import com.trackflow.shared.geografia.CoberturaLogistica;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CatalogoCiudadesControllerTest {

    private static final Ciudad MEDELLIN = new Ciudad(1L, "MEDELLÍN", "ANTIOQUIA");
    private static final Ciudad BELLO = new Ciudad(2L, "BELLO", "ANTIOQUIA");

    private final CatalogoDeCiudades ciudades = mock(CatalogoDeCiudades.class);
    private final CoberturaLogistica cobertura = mock(CoberturaLogistica.class);
    private MockMvc mvc;

    @BeforeEach
    void montar() {
        mvc = MockMvcBuilders.standaloneSetup(new CatalogoCiudadesController(ciudades, cobertura)).build();
        when(cobertura.ciudadesConCobertura()).thenReturn(Set.of(1L));
    }

    @Test
    void buscaCiudadesYDiceCualesTienenCobertura() throws Exception {
        when(ciudades.buscar("antio", 10)).thenReturn(List.of(MEDELLIN, BELLO));

        mvc.perform(get("/api/cities").param("q", "antio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("MEDELLÍN"))
                .andExpect(jsonPath("$[0].department").value("ANTIOQUIA"))
                .andExpect(jsonPath("$[0].label").value("MEDELLÍN - ANTIOQUIA"))
                .andExpect(jsonPath("$[0].covered").value(true))
                .andExpect(jsonPath("$[1].name").value("BELLO"))
                .andExpect(jsonPath("$[1].covered").value(false));
    }

    @Test
    void conOnlyCoveredPideMasCandidatosYDejaSoloLasCiudadesConCobertura() throws Exception {
        when(ciudades.buscar("antio", 100)).thenReturn(List.of(BELLO, MEDELLIN));

        mvc.perform(get("/api/cities").param("q", "antio").param("onlyCovered", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("MEDELLÍN"))
                .andExpect(jsonPath("$[0].covered").value(true));

        verify(ciudades).buscar("antio", 100);
    }

    @Test
    void nuncaDevuelveMasDeDiezCiudades() throws Exception {
        List<Ciudad> muchas = LongStream.rangeClosed(1, 15).mapToObj(id -> new Ciudad(id, "C" + id, "D")).toList();
        when(cobertura.ciudadesConCobertura()).thenReturn(Set.copyOf(LongStream.rangeClosed(1, 15).boxed().toList()));
        when(ciudades.buscar(eq("c"), anyInt())).thenReturn(muchas);

        mvc.perform(get("/api/cities").param("q", "c").param("onlyCovered", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10));
    }

    @Test
    void sinElTextoABuscarResponde400() throws Exception {
        mvc.perform(get("/api/cities")).andExpect(status().isBadRequest());
    }

    @Test
    void devuelveElDetalleDeUnaCiudad() throws Exception {
        when(ciudades.exigir(1L)).thenReturn(MEDELLIN);
        when(cobertura.tieneCobertura(1L)).thenReturn(true);

        mvc.perform(get("/api/cities/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("MEDELLÍN - ANTIOQUIA"))
                .andExpect(jsonPath("$.covered").value(true));
    }

    @Test
    void unaCiudadQueNoEstaEnElCatalogoResponde404ConElDetalle() throws Exception {
        when(ciudades.exigir(any())).thenCallRealMethod();
        when(ciudades.porId(99L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/cities/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Ciudad no encontrada en el catálogo"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(containsString("La ciudad 99")));
    }
}
