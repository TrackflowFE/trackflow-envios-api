package com.trackflow.bootstrap.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestClient;

/**
 * De dónde sale el correo de recuperación (HU-10). Hay tres salidas, y se escoge la
 * primera que esté configurada.
 *
 * 1. La API del proveedor por HTTPS. Es la que funciona en el despliegue: la
 *    plataforma bloquea el tráfico saliente a los puertos de SMTP, así que por ahí no
 *    sale nada, mientras que el 443 es el de cualquier página web.
 * 2. Un servidor SMTP. Sigue siendo lo cómodo en local, donde un servidor de correo
 *    de pruebas en un contenedor permite ver el mensaje entero sin mandar nada a nadie.
 * 3. La bitácora. Sin nada configurado, el mensaje se escribe en el log en vez de
 *    romper el arranque; el aviso deja claro que falta configurarlo.
 */
@Configuration
// Habilita el envío en segundo plano (ver CorreoDeRecuperacion): hablar con el
// proveedor dentro de la petición la hacía tardar segundos solo cuando la cuenta
// existía, y eso delataba cuáles existen.
@EnableAsync
public class CorreoConfig {

    private static final Logger log = LoggerFactory.getLogger(CorreoConfig.class);

    /**
     * El host de SMTP se comprueba aparte del bean: la autoconfiguración de Spring crea
     * el JavaMailSender en cuanto la propiedad existe, aunque esté vacía, y entonces
     * cada envío fallaría al intentar conectarse a ninguna parte.
     */
    @Bean
    EnvioDeCorreo envioDeCorreo(ObjectProvider<JavaMailSender> mailSender, RestClient.Builder clientes,
            @Value("${spring.mail.host:}") String hostSmtp,
            @Value("${trackflow.claves.api-url:}") String apiUrl,
            @Value("${trackflow.claves.api-clave:}") String apiClave,
            ClaveProperties propiedades) {

        if (!apiClave.isBlank() && !apiUrl.isBlank()) {
            log.info("Correo por API en {}: los enlaces de recuperación se envían de verdad", apiUrl);
            return new CorreoPorApi(clientes, apiUrl, apiClave, propiedades.remitente());
        }

        JavaMailSender emisor = mailSender.getIfAvailable();
        if (emisor != null && !hostSmtp.isBlank()) {
            log.info("Correo por SMTP en {}: los enlaces de recuperación se envían de verdad", hostSmtp);
            return porSmtp(emisor, propiedades);
        }

        log.warn("Sin proveedor de correo configurado: los enlaces de recuperación no se envían, solo se "
                + "escriben en la bitácora. Defina TRACKFLOW_CORREO_API_CLAVE en cualquier entorno real.");
        return (destino, asunto, cuerpo) ->
                log.info("Correo no enviado, sin proveedor. Para: {} | {}{}{}", destino, asunto,
                        System.lineSeparator(), cuerpo);
    }

    private EnvioDeCorreo porSmtp(JavaMailSender emisor, ClaveProperties propiedades) {
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
