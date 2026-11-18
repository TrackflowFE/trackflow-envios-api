package com.trackflow.bootstrap.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Ciclo de vida de los enlaces de recuperación (HU-10): emitirlos, validarlos y
 * consumirlos.
 *
 * El valor que viaja en el correo se entrega una sola vez y no se guarda: de él solo
 * queda la huella SHA-256. Quien pudiera leer la tabla no obtendría ningún enlace
 * utilizable, que es justo lo que se busca.
 */
@Service
public class EnlacesDeRecuperacion {

    static final String ENLACE_INVALIDO = "El enlace no es válido o ya fue usado";
    static final String ENLACE_VENCIDO = "El enlace ya venció: solicite uno nuevo";

    /** 32 bytes de aleatoriedad: adivinar un enlace no es una vía realista. */
    private static final int BYTES_DEL_TOKEN = 32;

    /** Se reutiliza: crear un SecureRandom por llamada es costoso y no aporta azar. */
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final SolicitudDeClaveRepository solicitudes;
    private final ClaveProperties propiedades;
    private final Clock clock;

    public EnlacesDeRecuperacion(SolicitudDeClaveRepository solicitudes, ClaveProperties propiedades, Clock clock) {
        this.solicitudes = solicitudes;
        this.propiedades = propiedades;
        this.clock = clock;
    }

    /**
     * Emite un enlace para ese usuario y deja sin efecto los que tuviera pendientes:
     * si no, cada solicitud sumaría una puerta abierta durante media hora.
     *
     * @return el token en claro, lo único que no queda almacenado en ninguna parte
     */
    @Transactional
    public String emitirPara(String username) {
        Instant ahora = clock.instant();
        solicitudes.borrarVencidas(ahora);
        solicitudes.borrarPendientesDe(username);

        String token = nuevoToken();
        solicitudes.save(new SolicitudDeClave(huella(token), username, ahora,
                ahora.plus(propiedades.vigenciaDelEnlace())));
        return token;
    }

    /**
     * Comprueba el enlace y lo marca usado. Si la operación que viene después falla,
     * la transacción se deshace y el enlace sigue sirviendo: no se pierde por un
     * intento con una clave que no cumplía la política.
     *
     * @return el usuario al que pertenece el enlace
     */
    @Transactional
    public String consumir(String token) {
        SolicitudDeClave solicitud = solicitudes.findById(huella(token))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, ENLACE_INVALIDO));

        if (solicitud.usada()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ENLACE_INVALIDO);
        }
        Instant ahora = clock.instant();
        if (solicitud.vencida(ahora)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ENLACE_VENCIDO);
        }

        solicitud.marcarUsada(ahora);
        return solicitud.getUsername();
    }

    private static String nuevoToken() {
        byte[] bytes = new byte[BYTES_DEL_TOKEN];
        ALEATORIO.nextBytes(bytes);
        // Sin relleno ni caracteres que haya que escapar dentro de una URL.
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 basta y sobra: el token ya es aleatorio, así que aquí no hay nada que
     * adivinar por fuerza bruta y no hace falta el coste deliberado de BCrypt.
     */
    private static String huella(String token) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha256.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 es obligatorio en toda JVM; si faltara, no habría nada que hacer.
            throw new IllegalStateException("Esta JVM no ofrece SHA-256", e);
        }
    }
}
