package com.trackflow.bootstrap.security;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuración de la recuperación de contraseña (HU-10), tomada del entorno.
 *
 * La dirección del enlace es del frontend, no de esta API: quien recibe el correo
 * abre una página donde escribe su nueva clave, y esa página es la que llama aquí.
 */
@Component
public class ClaveProperties {

    private final Duration vigenciaDelEnlace;
    private final String urlRestablecer;
    private final String remitente;

    public ClaveProperties(
            @Value("${trackflow.claves.recuperacion-minutos:30}") long recuperacionMinutos,
            @Value("${trackflow.claves.url-restablecer:http://localhost:5173/restablecer-clave.html}")
            String urlRestablecer,
            @Value("${trackflow.claves.remitente:no-responder@trackflow.app}") String remitente) {
        this.vigenciaDelEnlace = Duration.ofMinutes(recuperacionMinutos);
        this.urlRestablecer = urlRestablecer;
        this.remitente = remitente;
    }

    /** Media hora por defecto, como pide la historia. */
    public Duration vigenciaDelEnlace() {
        return vigenciaDelEnlace;
    }

    public String remitente() {
        return remitente;
    }

    /** El enlace que viaja en el correo, con la clave de un solo uso. */
    public String enlaceCon(String token) {
        String separador = urlRestablecer.contains("?") ? "&" : "?";
        return urlRestablecer + separador + "token=" + token;
    }
}
