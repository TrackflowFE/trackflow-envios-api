package com.trackflow.bootstrap.security;

import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea al arrancar las tres cuentas de la operación —operador, supervisor y
 * administrador— a partir de las claves y los correos del entorno, guardando solo el
 * hash de la contraseña. Sin ellas no habría con quién entrar; las demás se crean
 * desde la API (HU-11).
 *
 * Si la clave del entorno cambia, se actualiza el hash: rotar una clave sigue siendo
 * cambiar una variable en Render. Lo que no se toca es si el usuario está activo —si
 * alguien lo desactivó, reiniciar no lo reactiva—, ni la clave que su propio titular
 * haya cambiado (HU-10), ni un correo ya registrado desde la API.
 */
@Component
public class UsuariosIniciales implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UsuariosIniciales.class);

    private final UsuarioRepository usuarios;
    private final SecurityProperties propiedades;
    private final CorreosIniciales correos;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public UsuariosIniciales(UsuarioRepository usuarios, SecurityProperties propiedades, CorreosIniciales correos,
            PasswordEncoder encoder, Clock clock) {
        this.usuarios = usuarios;
        this.propiedades = propiedades;
        this.correos = correos;
        this.encoder = encoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        asegurar(propiedades.operadorUsuario(), propiedades.operadorClave(), SecurityProperties.ROL_OPERADOR,
                correos.operador());
        // El supervisor solo consulta: no hereda los permisos del operador.
        asegurar(propiedades.supervisorUsuario(), propiedades.supervisorClave(), SecurityProperties.ROL_SUPERVISOR,
                correos.supervisor());
        // Un solo rol le basta: la jerarquia de SecurityConfig hace que el
        // administrador pueda tambien lo del operador y lo del supervisor.
        asegurar(propiedades.adminUsuario(), propiedades.adminClave(), SecurityProperties.ROL_ADMIN,
                correos.admin());
    }

    private void asegurar(String username, String clave, String roles, String email) {
        if (clave == null || clave.isBlank()) {
            return;
        }

        usuarios.findById(username).ifPresentOrElse(
                usuario -> {
                    actualizarClave(usuario, clave);
                    // Solo siembra el correo si no hay ninguno: el que se haya definido
                    // desde la API es un dato de la operación, no del entorno.
                    if (tieneValor(email) && !tieneValor(usuario.getEmail())) {
                        asignarCorreo(usuario, email);
                    }
                },
                () -> {
                    Usuario nuevo = new Usuario(username, encoder.encode(clave), roles, true, clock.instant());
                    if (tieneValor(email)) {
                        asignarCorreo(nuevo, email);
                    }
                    usuarios.save(nuevo);
                    log.info("Usuario '{}' creado con roles {}", username, roles);
                });
    }

    private void actualizarClave(Usuario usuario, String clave) {
        // HU-10: si el dueño cambió su clave (o la recuperó por correo), manda la suya.
        // Sin esta salvedad, el siguiente reinicio la devolvería al valor del entorno y
        // el cambio se perdería sin que nadie lo note.
        if (usuario.isPasswordSetByUser()) {
            log.info("La clave del usuario '{}' la gobierna su titular: el entorno no la toca",
                    usuario.getUsername());
            return;
        }
        if (!encoder.matches(clave, usuario.getPasswordHash())) {
            usuario.cambiarPasswordHash(encoder.encode(clave));
            log.info("Clave del usuario '{}' actualizada desde el entorno", usuario.getUsername());
        }
    }

    /**
     * El correo identifica la cuenta al recuperar la clave, así que no puede repetirse.
     * Si dos variables traen el mismo valor, se avisa y no se asigna: guardarlo
     * rompería el índice único y con él el arranque entero.
     */
    private void asignarCorreo(Usuario usuario, String email) {
        String limpio = email.trim();
        boolean deOtraCuenta = usuarios.findByEmailIgnoreCase(limpio)
                .filter(otro -> !otro.getUsername().equals(usuario.getUsername()))
                .isPresent();
        if (deOtraCuenta) {
            log.warn("El correo configurado para '{}' ya pertenece a otra cuenta: se deja sin asignar",
                    usuario.getUsername());
            return;
        }

        usuario.cambiarEmail(limpio);
        log.info("Correo de contacto de '{}' tomado del entorno", usuario.getUsername());
    }

    private static boolean tieneValor(String texto) {
        return texto != null && !texto.isBlank();
    }
}
