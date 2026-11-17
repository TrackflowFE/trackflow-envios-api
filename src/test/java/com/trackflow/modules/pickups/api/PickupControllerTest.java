package com.trackflow.modules.pickups.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.trackflow.bootstrap.ValidationExceptionHandler;
import com.trackflow.modules.pickups.application.AdmitirRecogida;
import com.trackflow.modules.pickups.application.ConsultarRecogidas;
import com.trackflow.modules.pickups.application.OperadoresDisponibles;
import com.trackflow.modules.pickups.application.RecogidaSolicitada;
import com.trackflow.modules.pickups.application.VistaDeRecogida;
import com.trackflow.modules.pickups.domain.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PickupControllerTest {
    private final AdmitirRecogida admitir = mock(AdmitirRecogida.class);
    private final ConsultarRecogidas consultar = mock(ConsultarRecogidas.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new PickupController(admitir,
            consultar, mock(OperadoresDisponibles.class), mock(HorarioDeRecogida.class)))
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
                "REQUESTED", Instant.parse("2026-11-16T12:00:00Z"), "ABCDEFGH"));
        mvc.perform(post("/api/pickups").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.pickupNumber").value("RC0123456789AB"))
                .andExpect(jsonPath("$.managementCode").value("ABCDEFGH"));
    }

    @Test
    void devuelveMotivoDeFranjaInvalida() throws Exception {
        when(admitir.crear(any())).thenThrow(new FranjaDesconocidaException("08:00-10:00", List.of("10:00-12:00")));
        mvc.perform(post("/api/pickups").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("La franja no existe en el horario de atención"));
    }

    @Test
    void traduceCadaMotivoDeHorario() throws Exception {
        LocalDate fecha = LocalDate.of(2027, 1, 1);
        RuntimeException[] errores = {
                new FechaDeRecogidaEnElPasadoException(fecha, fecha.plusDays(1)),
                new DiaNoOperativoException(fecha),
                new FechaFueraDelHorizonteException(fecha, 30),
                new SinAnticipacionSuficienteException("08:00-10:00", Duration.ofHours(2))
        };
        int[] estados = {400, 422, 422, 422};
        String[] titulos = {"La fecha de la recogida ya pasó", "No se recoge ese día",
                "La fecha está fuera del horizonte de programación", "No hay tiempo para atender esa franja"};
        for (int i = 0; i < errores.length; i++) {
            doThrow(errores[i]).when(admitir).crear(any());
            mvc.perform(post("/api/pickups").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                    .andExpect(status().is(estados[i])).andExpect(jsonPath("$.title").value(titulos[i]))
                    .andExpect(jsonPath("$.detail").value(errores[i].getMessage()));
        }
    }

    @Test
    void conflictosDeGuiaEnCreacionYVinculacionSon409() throws Exception {
        String guia = "TF0123456789AB";
        for (RuntimeException error : List.of(new EnvioYaVinculadoException(guia), new EnvioYaEnRecorridoException(guia))) {
            doThrow(error).when(admitir).crear(any());
            mvc.perform(post("/api/pickups").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(error.getMessage()));
            doThrow(error).when(admitir).vincular(anyString(), anyString(), anyString());
            mvc.perform(put("/api/pickups/RC0123456789AB/shipment").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"trackingNumber\":\"" + guia + "\"}"))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(error.getMessage()));
        }
    }

    @Test
    void devuelveDatosPrivadosSoloAlAsignado() throws Exception {
        Instant now = Instant.parse("2026-11-16T12:00:00Z");
        Pickup pickup = Pickup.registrar(PickupNumber.of("RC0123456789AB"), "Dirección privada", 1L,
                "Ana", "3001234567", LocalDate.of(2026, 11, 17), "08:00-10:00",
                PackageSize.MEDIUM, BigDecimal.ONE, null, null, "remitente", now);
        pickup.reprogramar(pickup.getScheduledDate(), pickup.getTimeSlot(), "asignado", now);
        PickupEvent evento = new PickupEvent("e", "RC0123456789AB", PickupEventType.CANCELLED,
                PickupStatus.REQUESTED, PickupStatus.CANCELLED, "motivo privado", "asignado", now);
        when(consultar.una(eq("RC0123456789AB"), anyString())).thenAnswer(inv ->
                new VistaDeRecogida(pickup, "Ciudad", List.of(evento), pickup.puedeVerDatosPrivados(inv.getArgument(1))));
        for (String usuario : List.of("asignado", "otro")) {
            Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(usuario).build();
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(jwt, ""));
            try {
                var resultado = mvc.perform(get("/api/pickups/RC0123456789AB"))
                        .andExpect(status().isOk()).andExpect(jsonPath("$.accessRestricted").value(!usuario.equals("asignado")));
                if (usuario.equals("asignado")) {
                    resultado.andExpect(jsonPath("$.address").value("Dirección privada"))
                            .andExpect(jsonPath("$.contactName").value("Ana"))
                            .andExpect(jsonPath("$.contactPhone").value("3001234567"))
                            .andExpect(jsonPath("$.history[0].detail").value("motivo privado"));
                } else {
                    resultado.andExpect(jsonPath("$.address").doesNotExist())
                            .andExpect(jsonPath("$.contactName").doesNotExist())
                            .andExpect(jsonPath("$.contactPhone").doesNotExist())
                            .andExpect(jsonPath("$.history[0].detail").doesNotExist());
                }
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }
}
