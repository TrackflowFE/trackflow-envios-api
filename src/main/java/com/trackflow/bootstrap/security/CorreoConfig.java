package com.trackflow.bootstrap.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * De dónde sale el correo de recuperación (HU-10).
 *
 * Con un servidor SMTP configurado se envía de verdad. Sin él, en lugar de romper el
 * arranque, el mensaje se escribe en la bitácora: en local eso basta para recorrer el
 * flujo completo, y el aviso del arranque deja claro que falta configurarlo.
 */
@Configuration
public class CorreoConfig {

    private static final Logger log = LoggerFactory.getLogger(CorreoConfig.class);

    /**
     * El host se comprueba aparte del bean: la autoconfiguración de Spring crea el
     * JavaMailSender en cuanto la propiedad existe, aunque esté vacía, y entonces
     * cada envío fallaría al intentar conectarse a ninguna parte.
     */
    @Bean
    EnvioDeCorreo envioDeCorreo(ObjectProvider<JavaMailSender> mailSender,
            @Value("${spring.mail.host:}") String host, ClaveProperties propiedades) {
        JavaMailSender emisor = mailSender.getIfAvailable();
        if (emisor == null || host.isBlank()) {
            log.warn("Sin servidor SMTP configurado (TRACKFLOW_SMTP_HOST): los correos de recuperación "
                    + "no se envían, solo se escriben en la bitácora. Configúrelo en cualquier entorno real.");
            return (destino, asunto, cuerpo) ->
                    log.info("Correo no enviado, sin SMTP. Para: {} | {}{}{}", destino, asunto,
                            System.lineSeparator(), cuerpo);
        }

        log.info("Correo por SMTP activo en {}: los enlaces de recuperación se envían de verdad", host);
        return (destino, asunto, cuerpo) -> {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(propiedades.remitente());
            mensaje.setTo(destino);
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            emisor.send(mensaje);
        };
    }
}
