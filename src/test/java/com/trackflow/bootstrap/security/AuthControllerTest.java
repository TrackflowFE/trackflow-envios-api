package com.trackflow.bootstrap.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.server.ResponseStatusException;

/** HU-08: ingreso, consulta y cierre de sesión, firmando y verificando tokens reales. */
class AuthControllerTest {

    private static final Instant AHORA = Instant.now();
    private static final String SECRETO = "0123456789abcdef0123456789abcdef";

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final Sesiones sesiones = mock(Sesiones.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);
    private final SecurityProperties propiedades = new SecurityProperties(SECRETO, "operador", "clave-op", "admin",
            "", 480, 30);
    private final JwtConfig jwt = new JwtConfig(propiedades);
    private final AuthController controller = new AuthController(propiedades, jwt.jwtEncoder(), usuarios, sesiones,
            encoder, clock);

    @BeforeEach
    void usuarios() {
        when(usuarios.findById(any())).thenReturn(Optional.empty());
        when(usuarios.findById("operador")).thenReturn(
                Optional.of(new Usuario("operador", encoder.encode("clave-op"), "OPERADOR", true, AHORA)));
        when(usuarios.findById("inactivo")).thenReturn(
                Optional.of(new Usuario("inactivo", encoder.encode("clave-in"), "OPERADOR", false, AHORA)));
    }

    @Test
    void conCredencialesCorrectasEmiteUnTokenFirmadoYAbreLaSesion() {
        AuthController.TokenResponse respuesta = controller.login(
                new AuthController.LoginRequest("operador", "clave-op"));

        assertEquals("Bearer", respuesta.type());
        assertEquals("OPERADOR", respuesta.roles());
        assertEquals(Duration.ofMinutes(480).toSeconds(), respuesta.expiresInSeconds());

        ArgumentCaptor<String> jti = ArgumentCaptor.forClass(String.class);
        verify(sesiones).abrir(jti.capture(), eq("operador"), eq(AHORA), eq(AHORA.plus(Duration.ofMinutes(480))));

        when(sesiones.validarYRenovar(jti.getValue())).thenReturn(true);
        JwtDecoder decoder = jwt.jwtDecoder(sesiones);
        Jwt token = decoder.decode(respuesta.token());
        assertEquals("operador", token.getSubject());
        assertEquals("trackflow", token.getClaimAsString("iss"));
        assertEquals("OPERADOR", token.getClaimAsString("roles"));
        assertEquals(jti.getValue(), token.getId());
    }

    @Test
    void unaClaveIncorrectaSeRechazaConElMensajeGenerico() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> controller.login(new AuthController.LoginRequest("operador", "otra")));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        assertEquals(AuthController.CREDENCIALES_INVALIDAS, e.getReason());
        verifyNoInteractions(sesiones);
    }

    @Test
    void unUsuarioInexistenteSeRechazaConElMismoMensaje() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> controller.login(new AuthController.LoginRequest("nadie", "clave-op")));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        assertEquals(AuthController.CREDENCIALES_INVALIDAS, e.getReason());
    }

    @Test
    void unUsuarioDesactivadoSeRechazaConElMismoMensajeAunqueLaClaveSeaCorrecta() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> controller.login(new AuthController.LoginRequest("inactivo", "clave-in")));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        assertEquals(AuthController.CREDENCIALES_INVALIDAS, e.getReason());
        verifyNoInteractions(sesiones);
    }

    @Test
    void sinCredencialesConfiguradasElIngresoNoEstaDisponible() {
        SecurityProperties abiertas = new SecurityProperties(SECRETO, "operador", "", "admin", "", 480, 30);
        AuthController sinProteccion = new AuthController(abiertas, jwt.jwtEncoder(), usuarios, sesiones, encoder,
                clock);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> sinProteccion.login(new AuthController.LoginRequest("operador", "clave-op")));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatusCode());
        verifyNoInteractions(sesiones);
    }

    @Test
    void laSesionDevuelveElUsuarioSusRolesYElVencimiento() {
        Instant vence = AHORA.plusSeconds(3600);
        Jwt token = Jwt.withTokenValue("t").header("alg", "HS256").subject("operador").claim("roles", "OPERADOR")
                .jti("jti-1").issuedAt(AHORA).expiresAt(vence).build();

        AuthController.SesionResponse sesion = controller.sesion(token);

        assertEquals(new AuthController.SesionResponse("operador", "OPERADOR", vence), sesion);
    }

    @Test
    void cerrarSesionCierraLaSesionDelToken() {
        Jwt token = Jwt.withTokenValue("t").header("alg", "HS256").subject("operador").jti("jti-1")
                .issuedAt(AHORA).expiresAt(AHORA.plusSeconds(60)).build();

        controller.logout(token);

        verify(sesiones).cerrar("jti-1");
    }
}
