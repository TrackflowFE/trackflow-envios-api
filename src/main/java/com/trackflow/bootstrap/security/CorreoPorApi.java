package com.trackflow.bootstrap.security;

import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Envía el correo llamando a la API del proveedor por HTTPS, en vez de hablar SMTP.
 *
 * No es una preferencia de estilo: la plataforma donde corre esta API bloquea el
 * tráfico saliente a los puertos de SMTP (25, 465 y 587) para que nadie use sus
 * servidores como fuente de correo basura. Los intentos no se rechazan, se descartan
 * en silencio, así que la conexión agota su plazo sin que el servidor de correo llegue
 * a enterarse. Por el 443, que es el de cualquier página web, no hay tal bloqueo.
 *
 * El formato del cuerpo es el de Brevo. Cambiar de proveedor sería reescribir esta
 * clase y nada más: el resto de la aplicación solo conoce {@link EnvioDeCorreo}.
 */
public class CorreoPorApi implements EnvioDeCorreo {

    private final RestClient cliente;
    private final String url;
    private final String remitente;

    public CorreoPorApi(String url, String clave, String remitente) {
        this.url = url;
        this.remitente = remitente;
        // El cliente se construye aqui en lugar de pedir un RestClient.Builder al
        // contexto: ese bean lo aporta una autoconfiguracion que este proyecto no
        // trae, y sin ella la aplicacion no arranca.
        this.cliente = RestClient.builder()
                .defaultHeader("api-key", clave)
                .defaultHeader("accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public void enviar(String destino, String asunto, String cuerpo) {
        cliente.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "sender", Map.of("email", remitente, "name", "TrackFlow"),
                        "to", List.of(Map.of("email", destino)),
                        "subject", asunto,
                        "textContent", cuerpo))
                .retrieve()
                // El cuerpo de la respuesta no aporta nada: trae el identificador del
                // mensaje, que no se usa. Lo que importa es que no haya error, y de eso
                // ya se encarga retrieve().
                .toBodilessEntity();
    }
}
