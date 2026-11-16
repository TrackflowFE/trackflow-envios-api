package com.trackflow.modules.pickups.api;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
final class IpDelCliente {
    private final int saltos;
    private final Set<String> proxies;

    IpDelCliente(@Value("${trackflow.pickups.limite.saltos-proxy:0}") int saltos,
            @Value("${trackflow.pickups.limite.proxies-confiables:}") String proxies) {
        if (saltos < 0 || saltos > 16) {
            throw new IllegalArgumentException("Los saltos de proxy deben estar entre 0 y 16");
        }
        this.saltos = saltos;
        this.proxies = Arrays.stream(proxies.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).map(IpDelCliente::literal)
                .collect(Collectors.toUnmodifiableSet());
        if (saltos > 0 && this.proxies.isEmpty()) {
            throw new IllegalArgumentException("Debe configurar los proxies confiables para usar X-Forwarded-For");
        }
    }

    String obtener(HttpServletRequest request) {
        ServletRequest original = request;
        while (original instanceof ServletRequestWrapper wrapper) {
            original = wrapper.getRequest();
        }
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
        if (saltos == 0 || !proxies.contains(remoto) || !(original instanceof HttpServletRequest http)) {
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
                if (!proxies.contains(literal(cadena[i].trim()))) {
                    return remoto;
                }
            }
            return literal(cadena[cadena.length - saltos].trim());
        } catch (IllegalArgumentException e) {
            return remoto;
        }
    }

    private static String literal(String valor) {
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
            return InetAddress.getByName(valor).getHostAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Dirección IP inválida", e);
        }
    }
}
