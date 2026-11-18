package com.trackflow.bootstrap.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestión de usuarios, roles y permisos (HU-11). Cuelga de /api/admin, que la
 * configuración de seguridad reserva al administrador, así que un supervisor que
 * escriba la dirección a mano recibe 403 y no ve ningún dato.
 */
@RestController
@RequestMapping("/api/admin/usuarios")
public class UsuariosAdminController {

    public record CrearUsuarioRequest(
            @NotBlank(message = "el nombre de usuario es obligatorio")
            @Pattern(regexp = "[a-zA-Z0-9._-]{3,50}",
                    message = "use entre 3 y 50 caracteres: letras, números, punto, guion o guion bajo")
            String username,
            @NotBlank(message = "la contraseña es obligatoria") String clave,
            @NotBlank(message = "indique al menos un rol") String roles,
            @Email(message = "el correo no tiene un formato válido") String email) {
    }

    /** Un correo vacío o ausente retira el que hubiera. */
    public record CambiarEmailRequest(
            @Email(message = "el correo no tiene un formato válido") String email) {
    }

    public record CambiarRolesRequest(@NotBlank(message = "indique al menos un rol") String roles) {
    }

    public record UsuarioResponse(String username, String roles, boolean activo, boolean bloqueado,
            Instant bloqueadoHasta, int intentosFallidos, String email) {

        static UsuarioResponse de(Usuario usuario, Instant ahora) {
            return new UsuarioResponse(usuario.getUsername(), usuario.getRoles(), usuario.isActive(),
                    usuario.bloqueado(ahora), usuario.getLockedUntil(), usuario.getFailedAttempts(),
                    usuario.getEmail());
        }
    }

    public record AuditoriaResponse(String actor, String accion, String objetivo, String detalle,
            Instant ocurridoEn) {
    }

    private final GestionDeUsuarios gestion;
    private final Auditorias auditorias;
    private final java.time.Clock clock;

    public UsuariosAdminController(GestionDeUsuarios gestion, Auditorias auditorias, java.time.Clock clock) {
        this.gestion = gestion;
        this.auditorias = auditorias;
        this.clock = clock;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        Instant ahora = clock.instant();
        return gestion.listar().stream().map(u -> UsuarioResponse.de(u, ahora)).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@AuthenticationPrincipal Jwt quien, @Valid @RequestBody CrearUsuarioRequest peticion) {
        Usuario creado = gestion.crear(actor(quien), peticion.username(), peticion.clave(), peticion.roles(),
                peticion.email());
        return UsuarioResponse.de(creado, clock.instant());
    }

    @PutMapping("/{username}/roles")
    public UsuarioResponse cambiarRoles(@AuthenticationPrincipal Jwt quien, @PathVariable String username,
            @Valid @RequestBody CambiarRolesRequest peticion) {
        return UsuarioResponse.de(gestion.cambiarRoles(actor(quien), username, peticion.roles()), clock.instant());
    }

    @PutMapping("/{username}/activo")
    public UsuarioResponse cambiarActivo(@AuthenticationPrincipal Jwt quien, @PathVariable String username,
            @RequestParam boolean valor) {
        return UsuarioResponse.de(gestion.cambiarActivo(actor(quien), username, valor), clock.instant());
    }

    /**
     * Define el correo por el que esa persona podrá recuperar su clave (HU-10). Lo
     * hace el administrador: dejar que cada quien cambie el suyo convertiría una
     * sesión ajena en un camino para apropiarse de la cuenta.
     */
    @PutMapping("/{username}/email")
    public UsuarioResponse cambiarEmail(@AuthenticationPrincipal Jwt quien, @PathVariable String username,
            @Valid @RequestBody CambiarEmailRequest peticion) {
        return UsuarioResponse.de(gestion.cambiarEmail(actor(quien), username, peticion.email()), clock.instant());
    }

    @PostMapping("/{username}/desbloquear")
    public UsuarioResponse desbloquear(@AuthenticationPrincipal Jwt quien, @PathVariable String username) {
        return UsuarioResponse.de(gestion.desbloquear(actor(quien), username), clock.instant());
    }

    /** El registro de quién cambió qué y cuándo que pide HU-11. */
    @GetMapping("/auditoria")
    public List<AuditoriaResponse> auditoria(@RequestParam(defaultValue = "100") int cuantos) {
        return auditorias.ultimos(Math.clamp(cuantos, 1, 500)).stream()
                .map(a -> new AuditoriaResponse(a.getActor(), a.getAccion(), a.getObjetivo(), a.getDetalle(),
                        a.getOcurridoEn()))
                .toList();
    }

    private static String actor(Jwt quien) {
        return quien == null ? Auditorias.SISTEMA : quien.getSubject();
    }
}
