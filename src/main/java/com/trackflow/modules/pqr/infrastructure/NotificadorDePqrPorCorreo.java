package com.trackflow.modules.pqr.infrastructure;

import com.trackflow.bootstrap.security.EnvioDeCorreo;
import com.trackflow.modules.pqr.application.PqrEstadoCambiado;
import com.trackflow.modules.pqr.application.PqrRadicada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Avisa por correo al solicitante (HU-19).
 *
 * Escucha después de confirmar la transacción: no se avisa de una PQR que luego no se
 * guardó. Sale en otro hilo y un fallo del proveedor solo se registra: radicar o
 * responder no puede fallar porque el correo no salió. La pantalla de consulta siempre
 * muestra el estado y la respuesta, así que el aviso es una comodidad, no el único canal.
 *
 * Ni el asunto ni el cuerpo llevan la descripción del caso, y el asunto no lleva nada
 * escrito por el solicitante.
 */
@Component
public class NotificadorDePqrPorCorreo {

    private static final Logger log = LoggerFactory.getLogger(NotificadorDePqrPorCorreo.class);

    static final String ASUNTO_RADICADA = "TrackFlow: recibimos su PQR";
    static final String ASUNTO_ACTUALIZADA = "TrackFlow: su PQR cambió de estado";

    private final EnvioDeCorreo correo;

    public NotificadorDePqrPorCorreo(EnvioDeCorreo correo) {
        this.correo = correo;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alRadicar(PqrRadicada evento) {
        enviar(evento.radicado(), evento.email(), ASUNTO_RADICADA, cuerpoRadicada(evento));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCambiarElEstado(PqrEstadoCambiado evento) {
        enviar(evento.radicado(), evento.email(), ASUNTO_ACTUALIZADA, cuerpoActualizada(evento));
    }

    private void enviar(String radicado, String destino, String asunto, String cuerpo) {
        try {
            correo.enviar(destino, asunto, cuerpo);
        } catch (RuntimeException e) {
            log.error("No se pudo enviar el correo de la PQR {}", radicado, e);
        }
    }

    static String cuerpoRadicada(PqrRadicada evento) {
        return """
                Hola, %s:

                Recibimos su %s. Su número de radicado es:

                %s

                Guárdelo: para consultar el estado necesita el radicado y este mismo correo.

                Este mensaje es automático; no responda a esta dirección.
                """.formatted(sinSaltos(evento.nombre()), evento.tipo().name().toLowerCase(), evento.radicado());
    }

    static String cuerpoActualizada(PqrEstadoCambiado evento) {
        return """
                Hola, %s:

                Su PQR %s ahora está en estado %s.

                Para ver el detalle y la respuesta, si ya la hay, consulte con su radicado y este mismo correo.

                Este mensaje es automático; no responda a esta dirección.
                """.formatted(sinSaltos(evento.nombre()), evento.radicado(), evento.nuevo().name());
    }

    /** El nombre lo escribió el solicitante: no puede partir el mensaje en líneas. */
    private static String sinSaltos(String texto) {
        return texto.replaceAll("[\\r\\n\\t]+", " ");
    }
}
