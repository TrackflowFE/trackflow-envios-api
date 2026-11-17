package com.trackflow.modules.pickups.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.time.Clock;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.DefaultCorsProcessor;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class LimiteDeIntentosFilter extends OncePerRequestFilter {
    private static final String BASE = "/api/pickups/self-service";
    private static final int MAX_KEYS = 10000;
    private static final Pattern NUMERO = Pattern.compile("RC[0-9A-Z]{12}");
    private final VentanasDeIntentos ips;
    private final VentanasDeIntentos fallos;
    private final VentanasDeIntentos fallosPorIp;
    private final int limiteIp;
    private final int limiteRecogida;
    private final int limiteRecogidaPorIp;
    private final long ventanaMillis;
    private final CorsConfigurationSource cors;
    private final IpDelCliente ipDelCliente;

    public LimiteDeIntentosFilter(@Value("${trackflow.pickups.limite.ip:100}") int limiteIp,
            @Value("${trackflow.pickups.limite.recogida:30}") int limiteRecogida,
            @Value("${trackflow.pickups.limite.recogida-por-ip:5}") int limiteRecogidaPorIp,
            @Value("${trackflow.pickups.limite.ventana-segundos:900}") long segundos, Clock clock,
            CorsConfigurationSource cors, IpDelCliente ipDelCliente) {
        if (limiteIp < 1 || limiteRecogida < 1 || limiteRecogidaPorIp < 1 || segundos < 1 || segundos > 86400) {
            throw new IllegalArgumentException("Los límites de autoservicio deben ser positivos y la ventana de hasta 86400 segundos");
        }
        this.limiteIp = limiteIp;
        this.limiteRecogida = limiteRecogida;
        this.limiteRecogidaPorIp = limiteRecogidaPorIp;
        this.ventanaMillis = segundos * 1000;
        this.ips = new VentanasDeIntentos(MAX_KEYS, ventanaMillis, clock);
        this.fallos = new VentanasDeIntentos(MAX_KEYS, ventanaMillis, clock);
        this.fallosPorIp = new VentanasDeIntentos(MAX_KEYS, ventanaMillis, clock);
        this.cors = cors;
        this.ipDelCliente = ipDelCliente;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return "OPTIONS".equals(request.getMethod()) || !(path.equals(BASE) || path.startsWith(BASE + "/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        String path = request.getServletPath();
        String resto = path.length() > BASE.length() ? path.substring(BASE.length() + 1) : "";
        String numero = resto.split("/", 2)[0];
        int parametros = numero.indexOf(';');
        if (parametros >= 0) {
            numero = numero.substring(0, parametros);
        }
        boolean numeroValido = NUMERO.matcher(numero).matches();
        boolean cuentaRecogida = numeroValido && rutaDeCredencial(resto);
        String ip = ipDelCliente.obtener(request);
        String clavePorIp = ip + "|" + numero;
        Long reservaPorIp = null;
        Long reservaGlobal = null;
        boolean admitida = ips.reservar(ip, limiteIp) != null;
        if (admitida && cuentaRecogida) {
            reservaPorIp = fallosPorIp.reservar(clavePorIp, limiteRecogidaPorIp);
            admitida = reservaPorIp != null;
            if (admitida) {
                reservaGlobal = fallos.reservar(numero, limiteRecogida);
                admitida = reservaGlobal != null;
            }
        }
        if (!admitida) {
            fallosPorIp.liberar(clavePorIp, reservaPorIp);
            CorsConfiguration configuracion = cors.getCorsConfiguration(request);
            if (configuracion != null && !new DefaultCorsProcessor().processRequest(configuracion, request, response)) {
                return;
            }
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(Math.max(1, ventanaMillis / 1000)));
            response.setContentType("application/problem+json");
            response.getWriter().write("{\"title\":\"Demasiados intentos\",\"status\":429}");
            return;
        }
        HttpServletResponseWrapper captura = new HttpServletResponseWrapper(response);
        try {
            chain.doFilter(request, captura);
        } finally {
            if (cuentaRecogida && captura.getStatus() != 404) {
                fallosPorIp.liberar(clavePorIp, reservaPorIp);
                fallos.liberar(numero, reservaGlobal);
            }
        }
    }

    /**
     * Solo las rutas que autentican con el código de gestión cuentan contra la cuota de
     * la recogida. Un 404 de una subruta inventada no es un intento de credencial y no
     * debe servir para agotar la cuota del propietario.
     */
    private static boolean rutaDeCredencial(String resto) {
        String[] partes = resto.split("/", -1);
        if (partes.length > 2) {
            return false;
        }
        if (partes.length == 1) {
            return true;
        }
        String accion = partes[1];
        int parametros = accion.indexOf(';');
        if (parametros >= 0) {
            accion = accion.substring(0, parametros);
        }
        return "schedule".equals(accion) || "cancel".equals(accion);
    }
}
