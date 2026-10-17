package com.trackflow.bootstrap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackflow.modules.shipments.application.RepublicarEnviosCreados;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ReconstruccionControllerTest {

    private final RepublicarEnviosCreados republicar = mock(RepublicarEnviosCreados.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ReconstruccionController(republicar)).build();

    @Test
    void devuelveCuantosEnviosSeRepublicaron() throws Exception {
        when(republicar.ejecutar()).thenReturn(42);

        mvc.perform(post("/api/admin/rebuild-projections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipmentsRepublished").value(42));

        verify(republicar).ejecutar();
    }
}
