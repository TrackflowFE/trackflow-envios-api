package com.trackflow.bootstrap.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Redacta y envía el correo con el enlace de recuperación (HU-10).
 *
 * Está separado del servicio que cambia la clave para que el texto del mensaje se
 * pueda ajustar sin tocar nada de la lógica de seguridad.
 */
@Service
public class CorreoDeRecuperacion {

    private static final Logger log = LoggerFactory.getLogger(CorreoDeRecuperacion.class);

    private static final String ASUNTO = "TrackFlow: restablecer su contraseña";

    private final EnvioDeCorreo correo;
    private final ClaveProperties propiedades;

    public CorreoDeRecuperacion(EnvioDeCorreo correo, ClaveProperties propiedades) {
        this.correo = correo;
        this.propiedades = propiedades;
    }

    /**
     * Sale en un hilo aparte, y un fallo del servidor de correo no se propaga.
     *
     * Las dos cosas sostienen la misma regla: la respuesta a quien pide el enlace
     * tiene que ser idéntica exista o no la cuenta. Hablar con el servidor de correo
     * dentro de la petición la hacía tardar varios segundos solo cuando la cuenta
     * existía, y un error de SMTP la habría convertido en un 500 —dos maneras de
     * averiguar quién está registrado sin más que mirar la respuesta—.
     */
    @Async
    public void enviar(String destino, String username, String token) {
        try {
            correo.enviar(destino, ASUNTO, cuerpo(username, token));
        } catch (RuntimeException e) {
            // Queda aquí, donde solo lo ve quien opera el sistema, y no en la respuesta.
            log.error("No se pudo enviar el correo de recuperación de '{}'", username, e);
        }
    }

    private String cuerpo(String username, String token) {
        return """
                Hola, %s:

                Recibimos una solicitud para restablecer la contraseña de su cuenta de TrackFlow.
                Abra este enlace para elegir una nueva:

                %s

                El enlace vence en %d minutos y sirve una sola vez.
                Si no pidió este cambio, puede ignorar este mensaje: su contraseña sigue igual.
                """.formatted(username, propiedades.enlaceCon(token),
                propiedades.vigenciaDelEnlace().toMinutes());
    }
}
