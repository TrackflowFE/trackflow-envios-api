package com.trackflow.modules.pickups.api;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resuelve la IP del cliente para el límite de intentos.
 *
 * Los proxies confiables se declaran como IP literal o como rango CIDR
 * (10.0.0.0/8, fd00::/8). Render puede usar un rango y no una única dirección,
 * así que sin rangos todos los clientes compartirían la cuota del proxy.
 */
@Component
final class IpDelCliente {
    private static final Logger log = LoggerFactory.getLogger(IpDelCliente.class);

    private final int saltos;
    private final List<Rango> proxies;
    private final AtomicBoolean avisoProxySinSaltos = new AtomicBoolean();

    IpDelCliente(@Value("${trackflow.pickups.limite.saltos-proxy:0}") int saltos,
            @Value("${trackflow.pickups.limite.proxies-confiables:}") String proxies) {
        if (saltos < 0 || saltos > 16) {
            throw new IllegalArgumentException("Los saltos de proxy deben estar entre 0 y 16");
        }
        this.saltos = saltos;
        this.proxies = Arrays.stream(proxies.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).map(Rango::de)
                .toList();
        if (saltos > 0 && this.proxies.isEmpty()) {
            throw new IllegalArgumentException("Debe configurar los proxies confiables para usar X-Forwarded-For");
        }
    }

    String obtener(HttpServletRequest request) {
        ServletRequest original = request;
        while (original instanceof ServletRequestWrapper wrapper) {
            original = wrapper.getRequest();
        }
        HttpServletRequest http = original instanceof HttpServletRequest solicitud ? solicitud : null;
        String remoto;
        try {
            remoto = literal(original.getRemoteAddr());
        } catch (IllegalArgumentException e) {
            // getRemoteAddr normalmente es una IP literal, pero un contenedor puede
            // devolver una forma que no lo sea (por ejemplo, IPv6 con ámbito). No se
            // puede identificar al cliente, así que todas esas peticiones comparten
            // una única cuota en vez de romper la petición con un 500.
            return "desconocida";
        }
        if (saltos == 0) {
            avisarSiLlegaPorProxy(http);
            return remoto;
        }
        if (http == null || !esProxy(remoto)) {
            return remoto;
        }
        List<String> cabeceras = Collections.list(http.getHeaders("X-Forwarded-For"));
        if (cabeceras.isEmpty()) {
            return remoto;
        }
        String[] cadena = String.join(",", cabeceras).split(",", -1);
        if (cadena.length < saltos || cadena.length > 64) {
            return remoto;
        }
        try {
            for (int i = cadena.length - 1; i > cadena.length - saltos; i--) {
                if (!esProxy(cadena[i].trim())) {
                    return remoto;
                }
            }
            return literal(cadena[cadena.length - saltos].trim());
        } catch (IllegalArgumentException e) {
            return remoto;
        }
    }

    /**
     * Con saltos en 0 se ignora X-Forwarded-For. Si la cabecera aparece, la aplicación
     * está detrás de un proxy y todos los clientes comparten la cuota: se avisa una vez
     * para que se configuren los rangos en el despliegue.
     */
    private void avisarSiLlegaPorProxy(HttpServletRequest http) {
        if (http == null || !http.getHeaders("X-Forwarded-For").hasMoreElements()) {
            return;
        }
        if (avisoProxySinSaltos.compareAndSet(false, true)) {
            log.warn("Llegan peticiones con X-Forwarded-For pero saltos-proxy es 0: todos los clientes "
                    + "comparten la cuota del proxy. Configure TRACKFLOW_PICKUPS_LIMITE_SALTOS_PROXY y "
                    + "TRACKFLOW_PICKUPS_LIMITE_PROXIES_CONFIABLES con la IP o el rango CIDR del proxy.");
        }
    }

    private boolean esProxy(String valor) {
        try {
            byte[] direccion = bytes(valor);
            for (Rango rango : proxies) {
                if (rango.contiene(direccion)) {
                    return true;
                }
            }
            return false;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String literal(String valor) {
        try {
            return InetAddress.getByAddress(bytes(valor)).getHostAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Dirección IP inválida", e);
        }
    }

    private static byte[] bytes(String valor) {
        if (valor == null || valor.isEmpty() || valor.length() > 45
                || !valor.matches("[0-9A-Fa-f:.]+") || (!valor.contains(":") && !valor.matches("[0-9.]+"))) {
            throw new IllegalArgumentException("Dirección IP inválida");
        }
        try {
            if (!valor.contains(":")) {
                String[] partes = valor.split("\\.", -1);
                if (partes.length != 4) throw new IllegalArgumentException("Dirección IP inválida");
                for (String parte : partes) {
                    if (parte.isEmpty() || parte.length() > 3 || (parte.length() > 1 && parte.startsWith("0"))
                            || Integer.parseInt(parte) > 255) throw new IllegalArgumentException("Dirección IP inválida");
                }
            }
            return InetAddress.getByName(valor).getAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Dirección IP inválida", e);
        }
    }

    /** Una IP literal (prefijo completo) o un rango CIDR. */
    private record Rango(byte[] red, int prefijo) {

        static Rango de(String texto) {
            int corte = texto.indexOf('/');
            if (corte < 0) {
                byte[] direccion = bytes(texto);
                return new Rango(direccion, direccion.length * 8);
            }
            byte[] direccion = bytes(texto.substring(0, corte));
            int maximo = direccion.length * 8;
            String prefijoTexto = texto.substring(corte + 1);
            if (!prefijoTexto.matches("\\d{1,3}")) {
                throw new IllegalArgumentException("Prefijo CIDR inválido: " + texto);
            }
            int prefijo = Integer.parseInt(prefijoTexto);
            if (prefijo > maximo) {
                throw new IllegalArgumentException("Prefijo CIDR inválido: " + texto);
            }
            return new Rango(direccion, prefijo);
        }

        boolean contiene(byte[] direccion) {
            if (direccion.length != red.length) {
                return false;
            }
            int bytesCompletos = prefijo / 8;
            int bitsRestantes = prefijo % 8;
            for (int i = 0; i < bytesCompletos; i++) {
                if (red[i] != direccion[i]) {
                    return false;
                }
            }
            if (bitsRestantes == 0) {
                return true;
            }
            int mascara = (0xFF << (8 - bitsRestantes)) & 0xFF;
            return (red[bytesCompletos] & mascara) == (direccion[bytesCompletos] & mascara);
        }
    }
}
