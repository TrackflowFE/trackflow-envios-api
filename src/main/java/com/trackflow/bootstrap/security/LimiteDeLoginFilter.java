package com.trackflow.bootstrap.security;

import com.trackflow.shared.seguridad.IpDelCliente;
import com.trackflow.shared.seguridad.VentanasDeIntentos;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Límite de peticiones por IP a las rutas públicas de autenticación.
 *
 * El bloqueo de cuenta de HU-09 protege a un usuario concreto: cinco fallos y esa cuenta
 * queda cerrada. No protege al servicio, porque quien va rotando nombres de usuario nunca
 * acumula cinco fallos en ninguno y puede seguir indefinidamente. Este filtro pone el techo
 * que falta, y de paso cubre /clave/recuperar, donde cada petición manda un correo.
 *
 * Cada petición cuenta y no se libera: el límite es de peticiones, no de fallos, que es lo
 * que pide un control contra denegación de servicio.
 *
 * La cuenta es por IP. Con varias instancias detrás de un balanceador, cada una lleva su
 * propia cuenta y el techo real se multiplica por el número de instancias; centralizarlo
 * es trabajo del API Gateway, no de este filtro.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class LimiteDeLoginFilter extends OncePerRequestFilter {

    /** Rutas públicas donde una petición cuesta trabajo al servidor: clave, correo o token. */
    private static final Set<String> RUTAS = Set.of(
            "/api/auth/login",
            "/api/auth/clave/recuperar",
            "/api/auth/clave/restablecer");

    private static final int MAX_CLAVES = 10000;

    private final VentanasDeIntentos ventanas;
    private final IpDelCliente ipDelCliente;
    private final int limite;
    private final long segundos;

    public LimiteDeLoginFilter(@Value("${trackflow.auth.limite.ip:20}") int limite,
            @Value("${trackflow.auth.limite.ventana-segundos:60}") long segundos,
            Clock clock, IpDelCliente ipDelCliente) {
        if (limite < 1 || segundos < 1 || segundos > 86400) {
            throw new IllegalArgumentException(
                    "El límite de autenticación debe ser positivo y la ventana de hasta 86400 segundos");
        }
        this.limite = limite;
        this.segundos = segundos;
        this.ventanas = new VentanasDeIntentos(MAX_CLAVES, segundos * 1000, clock);
        this.ipDelCliente = ipDelCliente;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // El OPTIONS de precomprobación no lleva credenciales y no cuesta nada: si contara,
        // el navegador agotaría la cuota del usuario antes de su primer intento real.
        return "OPTIONS".equals(request.getMethod()) || !RUTAS.contains(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ip = ipDelCliente.obtener(request);
        if (ventanas.reservar(ip, limite) == null) {
            // 429 con Retry-After: un cliente legítimo sabe cuándo volver; el atacante ya
            // no recibe ninguna señal sobre si el usuario existe o si la clave era buena.
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(segundos));
            response.setHeader("Cache-Control", "no-store");
            response.setContentType("application/problem+json");
            response.getWriter().write("""
                    {"type":"about:blank","title":"Demasiadas peticiones","status":429,\
                    "detail":"Espere unos segundos antes de volver a intentarlo."}""");
            return;
        }
        chain.doFilter(request, response);
    }
}
