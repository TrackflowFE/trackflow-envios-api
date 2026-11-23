package com.trackflow.modules.pickups.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trackflow.modules.pickups.api.dto.CancelarRecogidaRequest;
import com.trackflow.modules.pickups.api.dto.HorarioRecogidaResponse;
import com.trackflow.modules.pickups.api.dto.RecogidaResumenResponse;
import com.trackflow.modules.pickups.api.dto.ReprogramarRecogidaRequest;
import com.trackflow.modules.pickups.api.dto.VincularEnvioRequest;
import com.trackflow.modules.pickups.application.AdmitirRecogida;
import com.trackflow.modules.pickups.application.ConsultarRecogidas;
import com.trackflow.modules.pickups.application.OperadoresDisponibles;
import com.trackflow.modules.pickups.application.VistaDeRecogida;
import com.trackflow.modules.pickups.domain.HorarioDeRecogida;
import com.trackflow.modules.pickups.domain.OperadorDesconocidoException;
import com.trackflow.modules.pickups.domain.PackageSize;
import com.trackflow.modules.pickups.domain.Pickup;
import com.trackflow.modules.pickups.domain.PickupNumber;
import com.trackflow.modules.pickups.domain.PickupStatus;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

class PickupControllerAccionesTest {

    private static final String NUMBER = "RC0123456789AB";
    private static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 17);

    private final AdmitirRecogida admitir = mock(AdmitirRecogida.class);
    private final ConsultarRecogidas consultar = mock(ConsultarRecogidas.class);
    private final OperadoresDisponibles operadores = mock(OperadoresDisponibles.class);
    private final HorarioDeRecogida horario = new HorarioDeRecogida(ZoneId.of("America/Bogota"),
            Set.of(DayOfWeek.SATURDAY, DayOfWeek.MONDAY), List.of("08:00-10:00", "10:00-12:00"),
            Duration.ofHours(4), 30);
    private final PickupController controller = new PickupController(admitir, consultar, operadores, horario);

    private final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("operador").build();

    @Test
    void elHorarioPublicoResumeDiasYFranjas() {
        HorarioRecogidaResponse respuesta = controller.horario();

        assertEquals(List.of(1, 6), respuesta.days());
        assertEquals("08:00", respuesta.open());
        assertEquals("12:00", respuesta.close());
        assertEquals(4, respuesta.leadTimeHours());
        assertEquals(30, respuesta.horizonDays());
        assertEquals("America/Bogota", respuesta.timeZone());
    }

    @Test
    void unHorarioSinFranjasNoTieneApertura() {
        HorarioDeRecogida vacio = new HorarioDeRecogida(ZoneId.of("UTC"), Set.of(DayOfWeek.MONDAY), List.of(),
                Duration.ZERO, 1);

        HorarioRecogidaResponse respuesta = HorarioRecogidaResponse.from(vacio);

        assertNull(respuesta.open());
        assertNull(respuesta.close());
    }

    @Test
    void losAsignablesSonLosOperadoresActivos() {
        when(operadores.activos()).thenReturn(List.of("operador", "supervisor"));

        assertEquals(List.of("operador", "supervisor"), controller.asignables());
    }

    @Test
    void listarSoloLasMiasUsaElUsuarioDeLaSesion() {
        Pickup pickup = Pickup.registrar(PickupNumber.of(NUMBER), "Calle 1", 1L, "Ana", "3001234567", FECHA,
                "08:00-10:00", PackageSize.MEDIUM, BigDecimal.ONE, null, null, "operador", NOW);
        pickup.reprogramar(FECHA, "08:00-10:00", "operador", NOW);
        when(consultar.listar(PickupStatus.SCHEDULED, "operador", true))
                .thenReturn(List.of(new VistaDeRecogida(pickup, "MEDELLÍN", List.of(), false)));

        List<RecogidaResumenResponse> lista = controller.listar(PickupStatus.SCHEDULED, "me", jwt);

        assertEquals(1, lista.size());
        RecogidaResumenResponse resumen = lista.get(0);
        assertEquals(NUMBER, resumen.pickupNumber());
        assertEquals("SCHEDULED", resumen.status());
        assertEquals("MEDELLÍN", resumen.cityName());
        assertEquals("operador", resumen.assignedUsername());
        assertTrue(resumen.accessRestricted());
    }

    @Test
    void listarSinSesionNiFiltroConsultaTodas() {
        when(consultar.listar(null, null, false)).thenReturn(List.of());

        assertTrue(controller.listar(null, null, null).isEmpty());
    }

    @Test
    void listarConUnFiltroDistintoDeMeSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> controller.listar(null, "otro", jwt));
        verifyNoInteractions(consultar);
    }

    @Test
    void reprogramarRespondeAceptadoConElActorDeLaSesion() {
        var respuesta = controller.reprogramar(NUMBER,
                new ReprogramarRecogidaRequest(FECHA, "10:00-12:00", "operador"), jwt);

        assertEquals(HttpStatus.ACCEPTED, respuesta.getStatusCode());
        verify(admitir).reprogramar(NUMBER, FECHA, "10:00-12:00", "operador", "operador");
    }

    @Test
    void cancelarSinCuerpoNiSesionUsaElActorDelSistema() {
        var respuesta = controller.cancelar(NUMBER, null, null);

        assertEquals(HttpStatus.ACCEPTED, respuesta.getStatusCode());
        verify(admitir).cancelar(NUMBER, null, PickupController.SIN_SESION);
    }

    @Test
    void cancelarConMotivoLoPasa() {
        controller.cancelar(NUMBER, new CancelarRecogidaRequest("Ya no lo necesito"), jwt);

        verify(admitir).cancelar(NUMBER, "Ya no lo necesito", "operador");
    }

    @Test
    void despacharCompletarYVincularRespondenAceptado() {
        assertEquals(HttpStatus.ACCEPTED, controller.despachar(NUMBER, jwt).getStatusCode());
        assertEquals(HttpStatus.ACCEPTED, controller.completar(NUMBER, jwt).getStatusCode());
        assertEquals(HttpStatus.ACCEPTED,
                controller.vincular(NUMBER, new VincularEnvioRequest("TF0123456789AB"), jwt).getStatusCode());

        verify(admitir).despachar(NUMBER, "operador");
        verify(admitir).completar(NUMBER, "operador");
        verify(admitir).vincular(NUMBER, "TF0123456789AB", "operador");
    }

    @Test
    void soloSePuedeAsignarAUnOperadorActivo() {
        OperadoresDisponibles disponibles = () -> List.of("operador");

        disponibles.exigir("operador");
        assertThrows(OperadorDesconocidoException.class, () -> disponibles.exigir("otro"));
        assertThrows(OperadorDesconocidoException.class, () -> disponibles.exigir(" "));
        assertThrows(OperadorDesconocidoException.class, () -> disponibles.exigir(null));
    }
}
