package com.trackflow.modules.chatbot.application;

import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * HU-21: contesta un mensaje con el contenido definido.
 *
 * Este caso de uso no conoce los envíos ni las cuentas: no tiene dependencia alguna
 * hacia ellos. Es lo que garantiza que un número de guía escrito en el chat no revele
 * datos de nadie. El texto del mensaje tampoco se guarda.
 */
@Service
public class ResponderMensaje {

    static final int MAXIMO_CARACTERES = 300;

    private final SesionesDeChat sesiones;
    private final CatalogoDeFaq catalogo;

    public ResponderMensaje(SesionesDeChat sesiones, CatalogoDeFaq catalogo) {
        this.sesiones = sesiones;
        this.catalogo = catalogo;
    }

    public RespuestaDelChat responder(UUID sesionId, String texto) {
        sesiones.admitir(sesionId);
        String mensaje = texto == null ? "" : texto.strip();
        if (mensaje.isEmpty() || mensaje.length() > MAXIMO_CARACTERES) {
            throw new IllegalArgumentException("El mensaje debe tener entre 1 y " + MAXIMO_CARACTERES + " caracteres.");
        }
        if (NumeroDeSeguimiento.contiene(mensaje)) {
            return RespuestaDelChat.numeroDeSeguimiento();
        }
        return catalogo.mejor(mensaje).map(c -> RespuestaDelChat.de(c.entrada()))
                .orElseGet(RespuestaDelChat::fueraDeTema);
    }
}
