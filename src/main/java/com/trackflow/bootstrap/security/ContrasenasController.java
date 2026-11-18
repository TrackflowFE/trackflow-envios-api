package com.trackflow.bootstrap.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cambio y recuperación de contraseña (HU-10).
 *
 * El cambio exige sesión y la clave actual; las dos operaciones de recuperación son
 * públicas por necesidad, porque quien las usa es justamente quien no puede entrar.
 */
@RestController
@RequestMapping("/api/auth/clave")
public class ContrasenasController {

    public record CambiarClaveRequest(
            @NotBlank(message = "indique su contraseña actual") String actual,
            @NotBlank(message = "indique la nueva contraseña") String nueva) {
    }

    public record RecuperarRequest(
            @NotBlank(message = "indique su correo")
            @Email(message = "el correo no tiene un formato válido") String email) {
    }

    public record RestablecerRequest(
            @NotBlank(message = "falta el token del enlace") String token,
            @NotBlank(message = "indique la nueva contraseña") String nueva) {
    }

    public record MensajeResponse(String mensaje) {
    }

    private final Contrasenas contrasenas;

    public ContrasenasController(Contrasenas contrasenas) {
        this.contrasenas = contrasenas;
    }

    /**
     * Cambia la clave del usuario del token. Al terminar, las demás sesiones de esa
     * persona quedan cerradas; la que hizo el cambio sigue sirviendo.
     */
    @PostMapping("/cambiar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CambiarClaveRequest peticion) {
        contrasenas.cambiar(jwt.getSubject(), jwt.getId(), peticion.actual(), peticion.nueva());
    }

    /**
     * Pide el enlace de recuperación. Responde siempre igual, exista o no la cuenta,
     * y por eso devuelve 202: lo único que confirma es que la solicitud se recibió.
     */
    @PostMapping("/recuperar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MensajeResponse recuperar(@Valid @RequestBody RecuperarRequest peticion) {
        contrasenas.solicitar(peticion.email());
        return new MensajeResponse(Contrasenas.RESPUESTA_UNIFORME);
    }

    /** Aplica la clave nueva con el token del enlace. */
    @PostMapping("/restablecer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restablecer(@Valid @RequestBody RestablecerRequest peticion) {
        contrasenas.restablecer(peticion.token(), peticion.nueva());
    }
}
