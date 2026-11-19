package com.trackflow.bootstrap.security;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Altas, cambios de rol y bajas de usuarios (HU-11).
 *
 * Dos reglas gobiernan todo lo que hay aquí: cualquier cambio queda auditado, y
 * cualquier cambio que reduzca los permisos de alguien cierra sus sesiones en el
 * acto, porque un token ya emitido seguiría llevando los permisos anteriores.
 */
@Service
public class GestionDeUsuarios {

    /** Los únicos roles que se pueden asignar; cualquier otro se rechaza. */
    private static final Set<String> ROLES_VALIDOS = Set.of(
            SecurityProperties.ROL_OPERADOR, SecurityProperties.ROL_SUPERVISOR, SecurityProperties.ROL_ADMIN);

    private final UsuarioRepository usuarios;
    private final Sesiones sesiones;
    private final Auditorias auditorias;
    private final PoliticaDeClaves politica;
    private final InvitacionDeCuenta invitacion;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public GestionDeUsuarios(UsuarioRepository usuarios, Sesiones sesiones, Auditorias auditorias,
            PoliticaDeClaves politica, InvitacionDeCuenta invitacion, PasswordEncoder encoder, Clock clock) {
        this.usuarios = usuarios;
        this.sesiones = sesiones;
        this.auditorias = auditorias;
        this.politica = politica;
        this.invitacion = invitacion;
        this.encoder = encoder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarios.findAll();
    }

    /**
     * @param email correo de contacto, opcional: sin él la persona no podrá recuperar
     *              su clave por correo (HU-10) y dependerá de un administrador
     */
    @Transactional
    public Usuario crear(String actor, String username, String clave, String roles, String email) {
        if (usuarios.existsById(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese nombre");
        }
        politica.exigir(clave);
        String normalizados = validarRoles(roles);

        Usuario usuario = new Usuario(username, encoder.encode(clave), normalizados, true, clock.instant());
        // La contrasena la eligio quien crea la cuenta, no su dueno: sirve para entrar
        // una vez y cambiarla, y para nada mas.
        usuario.exigirCambioDeClave();
        if (tieneValor(email)) {
            exigirCorreoLibre(email, username);
            usuario.cambiarEmail(email.trim());
        }
        usuarios.save(usuario);
        auditorias.registrar(actor, Auditorias.USUARIO_CREADO, username, "roles: " + normalizados);

        // Con correo, esa persona elige su propia contrasena y la inicial deja de
        // importar: nadie mas la conoce. Sin correo, el administrador tendra que
        // hacerle llegar la que acaba de fijar.
        if (tieneValor(email)) {
            invitacion.enviar(username, usuario.getEmail());
        }
        return usuario;
    }

    /**
     * Define o cambia el correo de contacto. Lo hace un administrador, no el propio
     * usuario: si cualquiera pudiera apuntar su cuenta a otro correo, el enlace de
     * recuperación llegaría a donde quisiera quien se hubiera colado en la sesión.
     */
    @Transactional
    public Usuario cambiarEmail(String actor, String username, String email) {
        Usuario usuario = buscar(username);
        String nuevo = tieneValor(email) ? email.trim() : null;
        if (Objects.equals(nuevo, usuario.getEmail())) {
            return usuario;
        }
        if (nuevo != null) {
            exigirCorreoLibre(nuevo, username);
        }

        usuario.cambiarEmail(nuevo);
        // El detalle no guarda el correo: el registro describe el cambio, no el dato.
        auditorias.registrar(actor, Auditorias.EMAIL_CAMBIADO, username,
                nuevo == null ? "correo retirado" : "correo de contacto definido");
        return usuario;
    }

    /** Dos cuentas con el mismo correo harían ambiguo a quién pertenece un enlace. */
    private void exigirCorreoLibre(String email, String username) {
        usuarios.findByEmailIgnoreCase(email.trim())
                .filter(otro -> !otro.getUsername().equals(username))
                .ifPresent(otro -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Ese correo ya está asociado a otra cuenta");
                });
    }

    private static boolean tieneValor(String texto) {
        return texto != null && !texto.isBlank();
    }

    @Transactional
    public Usuario cambiarRoles(String actor, String username, String roles) {
        Usuario usuario = buscar(username);
        String normalizados = validarRoles(roles);
        String anteriores = usuario.getRoles();
        if (normalizados.equals(anteriores)) {
            return usuario;
        }
        // Quitarle el rol al último administrador dejaría el sistema sin quien administre.
        if (esAdmin(anteriores) && !esAdmin(normalizados)) {
            exigirQueNoSeaElUltimoAdmin(username);
        }

        usuario.cambiarRoles(normalizados);
        // El token vigente seguiría anunciando los roles anteriores.
        int cerradas = sesiones.cerrarTodasDe(username, null);
        auditorias.registrar(actor, Auditorias.ROL_CAMBIADO, username,
                "de [" + anteriores + "] a [" + normalizados + "]; sesiones cerradas: " + cerradas);
        return usuario;
    }

    @Transactional
    public Usuario cambiarActivo(String actor, String username, boolean activo) {
        Usuario usuario = buscar(username);
        if (usuario.isActive() == activo) {
            return usuario;
        }
        if (!activo) {
            if (esAdmin(usuario.getRoles())) {
                exigirQueNoSeaElUltimoAdmin(username);
            }
            usuario.cambiarActivo(false);
            int cerradas = sesiones.cerrarTodasDe(username, null);
            auditorias.registrar(actor, Auditorias.USUARIO_DESACTIVADO, username,
                    "sesiones cerradas: " + cerradas);
            return usuario;
        }

        usuario.cambiarActivo(true);
        // Reactivar no debe dejarlo bloqueado por intentos viejos.
        usuario.desbloquear();
        auditorias.registrar(actor, Auditorias.USUARIO_ACTIVADO, username, null);
        return usuario;
    }

    @Transactional
    public Usuario desbloquear(String actor, String username) {
        Usuario usuario = buscar(username);
        if (!usuario.bloqueado(clock.instant()) && usuario.getFailedAttempts() == 0) {
            return usuario;
        }
        usuario.desbloquear();
        auditorias.registrar(actor, Auditorias.CUENTA_DESBLOQUEADA, username, null);
        return usuario;
    }

    private Usuario buscar(String username) {
        return usuarios.findById(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe ese usuario"));
    }

    /**
     * Un sistema sin administradores activos no se puede recuperar desde la API:
     * habría que entrar a la base de datos a mano.
     */
    private void exigirQueNoSeaElUltimoAdmin(String username) {
        boolean quedaOtro = usuarios.findAll().stream()
                .anyMatch(u -> u.isActive() && esAdmin(u.getRoles()) && !u.getUsername().equals(username));
        if (!quedaOtro) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Es el único administrador activo: asigne otro antes de quitarle el rol o desactivarlo");
        }
    }

    private static boolean esAdmin(String roles) {
        return List.of(roles.split(" ")).contains(SecurityProperties.ROL_ADMIN);
    }

    /** Acepta los roles en cualquier orden y mayúsculas, y los devuelve normalizados. */
    private static String validarRoles(String roles) {
        if (roles == null || roles.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indique al menos un rol");
        }
        List<String> pedidos = List.of(roles.trim().toUpperCase().split("\s+"));
        for (String rol : pedidos) {
            if (!ROLES_VALIDOS.contains(rol)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Rol desconocido: " + rol + ". Use OPERADOR, SUPERVISOR o ADMIN");
            }
        }
        return String.join(" ", pedidos.stream().distinct().sorted().toList());
    }
}
