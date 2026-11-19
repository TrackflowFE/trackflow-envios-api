package com.trackflow.modules.chatbot.api;

import com.trackflow.shared.seguridad.IpDelCliente;
import com.trackflow.shared.seguridad.RechazoPorLimite;
import com.trackflow.shared.seguridad.VentanasDeIntentos;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Tope por IP del chatbot: cuántas sesiones puede abrir y cuántos mensajes enviar.
 *
 * Complementa el tope por sesión (que está en SesionesDeChat): como una sesión es solo
 * un número al azar, quien quiera saltarse su límite puede abrir sesiones nuevas. Este
 * filtro cierra ese camino. Todo el rechazo es temporal: pasada la ventana vuelve a
 * responder.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 52)
public class LimiteDeChatFilter extends OncePerRequestFilter {

    private static final String SESIONES = "/api/chatbot/sesiones";
    private static final String MENSAJES = "/api/chatbot/mensajes";
    private static final int CAPACIDAD = 10000;

    private final VentanasDeIntentos sesiones;
    private final VentanasDeIntentos mensajes;
    private final int limiteSesiones;
    private final int limiteMensajes;
    private final long segundos;
    private final CorsConfigurationSource cors;
    private final IpDelCliente ipDelCliente;

    public LimiteDeChatFilter(@Value("${trackflow.chatbot.limite.sesiones-por-ip:10}") int limiteSesiones,
            @Value("${trackflow.chatbot.limite.mensajes-por-ip:60}") int limiteMensajes,
            @Value("${trackflow.chatbot.limite.ventana-segundos:60}") long segundos, Clock clock,
            CorsConfigurationSource cors, IpDelCliente ipDelCliente) {
        if (limiteSesiones < 1 || limiteMensajes < 1 || segundos < 1 || segundos > 86400) {
            throw new IllegalArgumentException(
                    "Los límites del chatbot deben ser positivos y la ventana de hasta 86400 segundos");
        }
        this.limiteSesiones = limiteSesiones;
        this.limiteMensajes = limiteMensajes;
        this.segundos = segundos;
        this.sesiones = new VentanasDeIntentos(CAPACIDAD, segundos * 1000, clock);
        this.mensajes = new VentanasDeIntentos(CAPACIDAD, segundos * 1000, clock);
        this.cors = cors;
        this.ipDelCliente = ipDelCliente;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getServletPath();
        return !"POST".equals(request.getMethod()) || !(SESIONES.equals(ruta) || MENSAJES.equals(ruta));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        boolean deSesion = SESIONES.equals(request.getServletPath());
        VentanasDeIntentos ventanas = deSesion ? sesiones : mensajes;
        int limite = deSesion ? limiteSesiones : limiteMensajes;
        if (ventanas.reservar(ipDelCliente.obtener(request), limite) == null) {
            RechazoPorLimite.responder(request, response, cors, segundos, "Demasiados mensajes");
            return;
        }
        chain.doFilter(request, response);
    }
}
