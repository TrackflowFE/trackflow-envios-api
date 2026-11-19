package com.trackflow.modules.pqr.api;

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
 * Frena el abuso de las dos rutas públicas de PQR, por IP.
 *
 * Radicar se limita para que nadie llene la bandeja de quien atiende ni el buzón de un
 * tercero (cada radicado envía un correo). Consultar se limita más holgado, porque
 * adivinar el correo de un radicado es lo que esta ruta permitiría sin tope.
 *
 * Cada petición cuenta, salga bien o mal: aquí no hay credencial que proteger de un
 * error ajeno, solo volumen.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 51)
public class LimiteDePqrFilter extends OncePerRequestFilter {

    private static final String RADICAR = "/api/pqr";
    private static final String CONSULTAR = "/api/pqr/consulta";
    private static final int CAPACIDAD = 10000;

    private final VentanasDeIntentos radicaciones;
    private final VentanasDeIntentos consultas;
    private final int limiteRadicar;
    private final int limiteConsultar;
    private final long segundos;
    private final CorsConfigurationSource cors;
    private final IpDelCliente ipDelCliente;

    public LimiteDePqrFilter(@Value("${trackflow.pqr.limite.radicar:5}") int limiteRadicar,
            @Value("${trackflow.pqr.limite.consultar:10}") int limiteConsultar,
            @Value("${trackflow.pqr.limite.ventana-segundos:900}") long segundos, Clock clock,
            CorsConfigurationSource cors, IpDelCliente ipDelCliente) {
        if (limiteRadicar < 1 || limiteConsultar < 1 || segundos < 1 || segundos > 86400) {
            throw new IllegalArgumentException(
                    "Los límites de PQR deben ser positivos y la ventana de hasta 86400 segundos");
        }
        this.limiteRadicar = limiteRadicar;
        this.limiteConsultar = limiteConsultar;
        this.segundos = segundos;
        this.radicaciones = new VentanasDeIntentos(CAPACIDAD, segundos * 1000, clock);
        this.consultas = new VentanasDeIntentos(CAPACIDAD, segundos * 1000, clock);
        this.cors = cors;
        this.ipDelCliente = ipDelCliente;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getServletPath();
        return !"POST".equals(request.getMethod()) || !(RADICAR.equals(ruta) || CONSULTAR.equals(ruta));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        boolean consulta = CONSULTAR.equals(request.getServletPath());
        VentanasDeIntentos ventanas = consulta ? consultas : radicaciones;
        int limite = consulta ? limiteConsultar : limiteRadicar;
        if (ventanas.reservar(ipDelCliente.obtener(request), limite) == null) {
            RechazoPorLimite.responder(request, response, cors, segundos, "Demasiados intentos");
            return;
        }
        chain.doFilter(request, response);
    }
}
