package com.trackflow.bootstrap.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Avisa a una persona de que tiene cuenta y la deja elegir su propia contraseña.
 *
 * Sin esto, el administrador fijaba la contraseña inicial y tenía que hacérsela llegar
 * por donde pudiera: un mensaje, un papel, de viva voz. Esa contraseña quedaba sabida
 * por dos personas y viajando por un canal que nadie controla. Con la invitación, quien
 * recibe la cuenta elige su clave y el administrador nunca llega a conocerla.
 *
 * Reutiliza los enlaces de un solo uso de HU-10: el mismo mecanismo, usado para
 * estrenar la cuenta en vez de para recuperarla.
 */
@Service
public class InvitacionDeCuenta {

    private static final Logger log = LoggerFactory.getLogger(InvitacionDeCuenta.class);

    private static final String ASUNTO = "Su cuenta de TrackFlow está lista";

    private final EnlacesDeRecuperacion enlaces;
    private final EnvioDeCorreo correo;
    private final ClaveProperties propiedades;

    public InvitacionDeCuenta(EnlacesDeRecuperacion enlaces, EnvioDeCorreo correo, ClaveProperties propiedades) {
        this.enlaces = enlaces;
        this.correo = correo;
        this.propiedades = propiedades;
    }

    /**
     * Sale en segundo plano y no propaga sus fallos: que el correo no salga no puede
     * deshacer la creación de la cuenta, que ya está hecha y auditada. Si falla, queda
     * en la bitácora y esa persona siempre puede pedir el enlace desde "olvidé mi
     * contraseña".
     */
    @Async
    public void enviar(String username, String email) {
        try {
            String token = enlaces.emitirPara(username);
            correo.enviar(email, ASUNTO, cuerpo(username, token));
        } catch (RuntimeException e) {
            log.error("No se pudo enviar la invitación de la cuenta '{}'", username, e);
        }
    }

    private String cuerpo(String username, String token) {
        return """
                Hola, %s:

                Le crearon una cuenta en TrackFlow. Su usuario es: %s

                Elija su contraseña aquí:

                %s

                El enlace vence en %d minutos y sirve una sola vez. Si se le pasa el plazo,
                puede pedir uno nuevo desde "¿Olvidaste tu contraseña?" en la pantalla de
                ingreso.
                """.formatted(username, username, propiedades.enlaceCon(token),
                propiedades.vigenciaDelEnlace().toMinutes());
    }
}
