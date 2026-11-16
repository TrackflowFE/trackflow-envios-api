package com.trackflow.modules.pickups.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.trackflow.bootstrap.ValidationExceptionHandler;
import com.trackflow.modules.pickups.application.AdmitirRecogida;
import com.trackflow.modules.pickups.application.ConsultarRecogidas;
import com.trackflow.modules.pickups.application.RecogidaSolicitada;
import com.trackflow.modules.pickups.domain.CodigoDeGestionInvalidoException;
import com.trackflow.modules.pickups.domain.ReprogramacionSinCambiosException;
import com.trackflow.modules.pickups.domain.TransicionDeRecogidaInvalidaException;
import com.trackflow.modules.pickups.domain.PickupStatus;
import com.trackflow.modules.pickups.domain.DiaNoOperativoException;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PickupSelfServiceControllerTest {
    private final AdmitirRecogida admitir = mock(AdmitirRecogida.class);
    private final ConsultarRecogidas consultar = mock(ConsultarRecogidas.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new PickupSelfServiceController(admitir, consultar))
            .setControllerAdvice(new ValidationExceptionHandler(), new PickupsExceptionHandler()).build();
    private static final String BODY = """
            {"address":"Calle 1 # 1-1","cityId":1,"contactName":"Ana Torres",
             "contactPhone":"3001234567","scheduledDate":"2027-01-01","timeSlot":"08:00-10:00",
             "packageSize":"MEDIUM","weightKg":5.5}
            """;

    @Test
    void creacionDevuelveCodigoYProhibeVincularGuiaAjena() throws Exception {
        when(admitir.crear(any())).thenReturn(new RecogidaSolicitada("e", "RC0123456789AB", "REQUESTED",
                Instant.parse("2026-11-16T12:00:00Z"), "ABCDEFGH"));
        mvc.perform(post("/api/pickups/self-service").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.managementCode").value("ABCDEFGH"));
        mvc.perform(post("/api/pickups/self-service").contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("\"weightKg\":5.5", "\"weightKg\":5.5,\"shipmentTrackingNumber\":\"TF0123456789AB\"")))
                .andExpect(status().isBadRequest());
        verify(admitir, times(1)).crear(any());
    }

    @Test
    void codigoIncorrectoYNumeroInexistenteDevuelvenMismo404() throws Exception {
        when(consultar.delRemitente(anyString(), any())).thenThrow(new CodigoDeGestionInvalidoException());
        String a = mvc.perform(get("/api/pickups/self-service/RC0123456789AB").header("X-Codigo-Gestion", "MAL"))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        String b = mvc.perform(get("/api/pickups/self-service/RC0123456789AC").header("X-Codigo-Gestion", "MAL"))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals(a.replace("RC0123456789AB", "NUMERO"),
                b.replace("RC0123456789AC", "NUMERO"));
    }

    @Test
    void remitenteNoPuedeAsignarOperador() throws Exception {
        mvc.perform(put("/api/pickups/self-service/RC0123456789AB/schedule")
                .header("X-Codigo-Gestion", "ABCDEFGH").contentType(MediaType.APPLICATION_JSON)
                .content("{\"scheduledDate\":\"2027-01-01\",\"timeSlot\":\"08:00-10:00\",\"assignedUsername\":\"admin\"}"))
                .andExpect(status().isAccepted());
        verify(admitir).reprogramarComoRemitente(eq("RC0123456789AB"), eq("ABCDEFGH"), any(), eq("08:00-10:00"));
    }

    @Test
    void reprogramacionYCancelacionTraducenMotivos() throws Exception {
        String numero = "RC0123456789AB";
        String horario = "{\"scheduledDate\":\"2027-01-01\",\"timeSlot\":\"08:00-10:00\"}";
        doThrow(new ReprogramacionSinCambiosException()).when(admitir)
                .reprogramarComoRemitente(eq(numero), anyString(), any(), anyString());
        mvc.perform(put("/api/pickups/self-service/{numero}/schedule", numero)
                .header("X-Codigo-Gestion", "ABCDEFGH").contentType(MediaType.APPLICATION_JSON).content(horario))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.title").value("Reprogramación sin cambios"));
        doThrow(new DiaNoOperativoException(LocalDate.of(2027, 1, 1))).when(admitir)
                .reprogramarComoRemitente(eq(numero), anyString(), any(), anyString());
        mvc.perform(put("/api/pickups/self-service/{numero}/schedule", numero)
                .header("X-Codigo-Gestion", "ABCDEFGH").contentType(MediaType.APPLICATION_JSON).content(horario))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.title").value("No se recoge ese día"));
        doThrow(new TransicionDeRecogidaInvalidaException(numero, PickupStatus.ON_THE_WAY, "cancelar"))
                .when(admitir).cancelarComoRemitente(eq(numero), anyString(), any());
        mvc.perform(post("/api/pickups/self-service/{numero}/cancel", numero).header("X-Codigo-Gestion", "ABCDEFGH"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(
                        "La recogida " + numero + " está en estado ON_THE_WAY y no se puede cancelar"));
    }
}
