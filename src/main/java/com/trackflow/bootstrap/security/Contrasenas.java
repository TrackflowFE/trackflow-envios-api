package com.trackflow.bootstrap.security;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Cambiar y recuperar la contraseña (HU-10).
 *
 * Tres reglas gobiernan todo lo que hay aquí:
 *
 * 1. Cambiar la clave cierra las demás sesiones de esa persona. Si la cambia porque
 *    sospecha que alguien la tenía, dejar vivas las sesiones abiertas con la clave
 *    anterior convertiría el cambio en un gesto vacío.
 * 2. Pedir un enlace responde siempre lo mismo, exista o no la cuenta: si el mensaje
 *    cambiara, el formulario serviría para averiguar quién está registrado.
 * 3. El enlace vale media hora y una sola vez, y de eso se encarga
 *    {@link EnlacesDeRecuperacion}.
 */
@Service
public class Contrasenas {

    private static final Logger log = LoggerFactory.getLogger(Contrasenas.class);

    /** Lo que se responde al pedir un enlace, haya cuenta o no. */
    public static final String RESPUESTA_UNIFORME =
            "Si el correo corresponde a una cuenta, le enviamos un enlace para restablecer la contraseña";

    static final String CLAVE_ACTUAL_NO_COINCIDE = "La contraseña actual no es correcta";

    private final UsuarioRepository usuarios;
    private final EnlacesDeRecuperacion enlaces;
    private final CorreoDeRecuperacion correo;
    private final Sesiones sesiones;
    private final Auditorias auditorias;
    private final PoliticaDeClaves politica;
    private final PasswordEncoder encoder;

    public Contrasenas(UsuarioRepository usuarios, EnlacesDeRecuperacion enlaces, CorreoDeRecuperacion correo,
            Sesiones sesiones, Auditorias auditorias, PoliticaDeClaves politica, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.enlaces = enlaces;
        this.correo = correo;
        this.sesiones = sesiones;
        this.auditorias = auditorias;
        this.politica = politica;
        this.encoder = encoder;
    }

    /**
     * Cambio de clave de quien ya tiene la sesión abierta.
     *
     * @param jtiActual sesión desde la que se hace el cambio, la única que sobrevive
     */
    @Transactional
    public void cambiar(String username, String jtiActual, String actual, String nueva) {
        Usuario usuario = usuarios.findById(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, CLAVE_ACTUAL_NO_COINCIDE));

        // Exigir la clave actual impide que un token robado sirva para apropiarse de
        // la cuenta cambiándole la contraseña.
        if (!encoder.matches(actual, usuario.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, CLAVE_ACTUAL_NO_COINCIDE);
        }

        politica.exigir(nueva, usuario.getPasswordHash());
        usuario.cambiarClavePropia(encoder.encode(nueva));

        int cerradas = sesiones.cerrarTodasDe(username, jtiActual);
        auditorias.registrar(username, Auditorias.CLAVE_CAMBIADA, username,
                "por su titular; otras sesiones cerradas: " + cerradas);
    }

    /**
     * Pide un enlace de recuperación. No devuelve nada distinto según el resultado:
     * el controlador responde siempre {@link #RESPUESTA_UNIFORME}.
     */
    @Transactional
    public void solicitar(String email) {
        Optional<Usuario> encontrado = usuarios.findByEmailIgnoreCase(email.trim());
        if (encontrado.isEmpty() || !encontrado.get().isActive()) {
            // Sin cuenta, o con la cuenta desactivada, no se envía nada. Queda constancia
            // en la bitácora del servidor, que no ve quien hizo la solicitud.
            log.info("Solicitud de recuperación para un correo sin cuenta activa: no se envía correo");
            return;
        }

        Usuario usuario = encontrado.get();
        String token = enlaces.emitirPara(usuario.getUsername());
        correo.enviar(usuario.getEmail(), usuario.getUsername(), token);
    }

    /** Aplica la clave nueva usando el enlace recibido por correo. */
    @Transactional
    public void restablecer(String token, String nueva) {
        String username = enlaces.consumir(token);
        Usuario usuario = usuarios.findById(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        EnlacesDeRecuperacion.ENLACE_INVALIDO));

        // Si la clave no cumple la política, esta transacción se deshace por completo
        // y el enlace vuelve a quedar sin usar, para intentarlo con otra.
        politica.exigir(nueva, usuario.getPasswordHash());
        usuario.cambiarClavePropia(encoder.encode(nueva));

        // Aquí se cierran todas, incluso la de quien pide el cambio: lo normal es que
        // haya llegado por el correo precisamente porque no podía entrar.
        int cerradas = sesiones.cerrarTodasDe(username, null);
        auditorias.registrar(username, Auditorias.CLAVE_CAMBIADA, username,
                "por recuperación con enlace; sesiones cerradas: " + cerradas);
    }
}
