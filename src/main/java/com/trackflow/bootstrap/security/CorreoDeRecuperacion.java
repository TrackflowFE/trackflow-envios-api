package com.trackflow.bootstrap.security;

import org.springframework.stereotype.Service;

/**
 * Redacta y envía el correo con el enlace de recuperación (HU-10).
 *
 * Está separado del servicio que cambia la clave para que el texto del mensaje se
 * pueda ajustar sin tocar nada de la lógica de seguridad.
 */
@Service
public class CorreoDeRecuperacion {

    private static final String ASUNTO = "TrackFlow: restablecer su contraseña";

    private final EnvioDeCorreo correo;
    private final ClaveProperties propiedades;

    public CorreoDeRecuperacion(EnvioDeCorreo correo, ClaveProperties propiedades) {
        this.correo = correo;
        this.propiedades = propiedades;
    }

    public void enviar(String destino, String username, String token) {
        correo.enviar(destino, ASUNTO, cuerpo(username, token));
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
