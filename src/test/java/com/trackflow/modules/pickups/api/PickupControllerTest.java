package com.trackflow.modules.pickups.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.trackflow.bootstrap.ValidationExceptionHandler;
import com.trackflow.modules.pickups.application.AdmitirRecogida;
import com.trackflow.modules.pickups.application.ConsultarRecogidas;
import com.trackflow.modules.pickups.application.OperadoresDisponibles;
import com.trackflow.modules.pickups.application.RecogidaSolicitada;
import com.trackflow.modules.pickups.domain.FranjaDesconocidaException;
import com.trackflow.modules.pickups.domain.HorarioDeRecogida;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PickupControllerTest {
    private final AdmitirRecogida admitir = mock(AdmitirRecogida.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new PickupController(admitir,
            mock(ConsultarRecogidas.class), mock(OperadoresDisponibles.class), mock(HorarioDeRecogida.class)))
            .setControllerAdvice(new ValidationExceptionHandler(), new PickupsExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();

    private static final String REQUEST = """
            {"address":"Calle 1 # 1-1","cityId":1,"contactName":"Ana Torres",
             "contactPhone":"3001234567","scheduledDate":"2027-01-01","timeSlot":"08:00-10:00",
             "packageSize":"MEDIUM","weightKg":5.5}
            """;

    @Test
    void devuelveNumeroAlAceptar() throws Exception {
        when(admitir.crear(any())).thenReturn(new RecogidaSolicitada("event", "RC0123456789AB",
                "REQUESTED", Instant.parse("2026-11-16T12:00:00Z")));
        mvc.perform(post("/api/pickups").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.pickupNumber").value("RC0123456789AB"));
    }

    @Test
    void devuelveMotivoDeFranjaInvalida() throws Exception {
        when(admitir.crear(any())).thenThrow(new FranjaDesconocidaException("08:00-10:00", List.of("10:00-12:00")));
        mvc.perform(post("/api/pickups").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("La franja no existe en el horario de atención"));
    }
}
