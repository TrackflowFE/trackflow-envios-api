package com.trackflow.bootstrap.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * La escritura exige un token de operador; la consulta es pública, porque el número de
 * seguimiento es la credencial del cliente, como en cualquier servicio de paquetería.
 *
 * Sin credenciales configuradas la protección queda desactivada, de modo que el entorno
 * local y las pruebas no necesitan token y el despliegue sí lo exige.
 */
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final SecurityProperties propiedades;

    public SecurityConfig(SecurityProperties propiedades) {
        this.propiedades = propiedades;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) {
        boolean protegido = propiedades.proteccionActiva();

        http
                // Sin esto, el filtro de seguridad rechaza las peticiones preflight antes de
                // que se aplique la configuración CORS de Spring MVC (ver CorsConfig).
                .cors(Customizer.withDefaults())
                // CSRF explota que el navegador adjunta solo las cookies de sesión. Aquí no
                // hay cookies: la sesión es STATELESS y el JWT viaja en la cabecera
                // Authorization, que un sitio ajeno no puede añadir. Por eso se desactiva.
                .csrf(AbstractHttpConfigurer::disable) // NOSONAR java:S4502 - API sin estado, JWT en cabecera, sin cookies
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    // El OPTIONS de precomprobación nunca lleva token, por diseño del propio
                    // HU-13: la ficha completa lleva datos personales sin enmascarar y queda auditada.
                    auth.requestMatchers(HttpMethod.GET, "/api/shipments/*/ficha/completa")
                            .hasRole(SecurityProperties.ROL_ADMIN);
                    // La ficha enmascarada la usa quien atiende el envío.
                    auth.requestMatchers(HttpMethod.GET, "/api/shipments/*/ficha")
                            .hasAnyRole(SecurityProperties.ROL_OPERADOR, SecurityProperties.ROL_SUPERVISOR,
                                    SecurityProperties.ROL_ADMIN);
                    // CORS: si una regla de rol lo alcanza (como pasaba con /api/admin/**, que
                    // no distinguía el método), el navegador ve un 401 en la precomprobación y
                    // ni siquiera intenta la petición real. Dejarlo pasar aquí no abre nada: la
                    // petición real —GET, POST— sigue evaluándose por las reglas de abajo.
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll();
                    // HU-10: pedir el enlace de recuperación y aplicar la clave nueva tienen
                    // que ser públicos, porque quien los usa es precisamente quien no puede
                    // entrar. Lo que protege el restablecimiento es el token del correo: vale
                    // media hora y una sola vez.
                    auth.requestMatchers(HttpMethod.POST, "/api/auth/clave/recuperar",
                            "/api/auth/clave/restablecer").permitAll();
                    // Cambiar la clave estando dentro solo exige un token válido, sea cual
                    // sea el rol; la clave actual la verifica la propia operación.
                    auth.requestMatchers(HttpMethod.POST, "/api/auth/clave/cambiar").authenticated();
                    // Consultar o cerrar la propia sesión solo exige un token válido, sea
                    // cual sea el rol.
                    auth.requestMatchers(HttpMethod.GET, "/api/auth/sesion").authenticated();
                    auth.requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated();
                    // Lo que ve el cliente final: el número de seguimiento es su credencial.
                    auth.requestMatchers(HttpMethod.GET, "/api/tracking/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/shipments/*/events", "/api/shipments/*/route")
                            .permitAll();
                    // El catálogo de ciudades son las capitales del país: no es información
                    // de la operación, y el formulario de registro lo usa para sugerir.
                    auth.requestMatchers(HttpMethod.GET, "/api/cities", "/api/cities/*").permitAll();
                    // El horario de las recogidas es el horario del servicio, no información
                    // de ninguna recogida: se puede ver sin token. Es la única ruta de
                    // recogidas que queda abierta; las demás se cierran más abajo.
                    auth.requestMatchers(HttpMethod.GET, "/api/pickups/operating-hours").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/pickups/self-service",
                            "/api/pickups/self-service/*/cancel").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/pickups/self-service/*").permitAll();
                    auth.requestMatchers(HttpMethod.PUT, "/api/pickups/self-service/*/schedule").permitAll();
                    // /error es a donde Spring reenvía las respuestas de error: si se
                    // cerrara, un 404 llegaría al cliente como 401.
                    auth.requestMatchers("/", "/error", "/actuator/health/**", "/swagger-ui.html",
                            "/swagger-ui/**", "/v3/api-docs/**").permitAll();

                    if (protegido) {
                        // Las operaciones de mantenimiento no las hace quien registra paquetes.
                        auth.requestMatchers("/api/admin/**").hasRole(SecurityProperties.ROL_ADMIN);
                        // Los reportes muestran la carga de la operación, no el estado de un
                        // envío: no hay número de seguimiento que haga de credencial, así que
                        // no son públicos como /api/tracking. El administrador hereda el rol.
                        // HU-11: los reportes son para supervision, no para quien registra.
                        auth.requestMatchers(HttpMethod.GET, "/api/reports/**")
                                .hasAnyRole(SecurityProperties.ROL_SUPERVISOR, SecurityProperties.ROL_ADMIN);
                        auth.requestMatchers(HttpMethod.POST, "/api/**")
                                .hasRole(SecurityProperties.ROL_OPERADOR);
                        // El estado interno del envío y la red de centros son de la
                        // operación, no del cliente: los consulta quien registra movimientos.
                        auth.requestMatchers(HttpMethod.GET, "/api/shipments/*/actions", "/api/centers",
                                "/api/centers/**").hasRole(SecurityProperties.ROL_OPERADOR);
                        // Las recogidas son de la operación: quién las pidió, dónde y en qué
                        // estado están es de los operadores. La dirección y el contacto solo
                        // los ve el operador asignado, y eso lo resuelve la aplicación.
                        auth.requestMatchers(HttpMethod.GET, "/api/pickups", "/api/pickups/*")
                                .hasRole(SecurityProperties.ROL_OPERADOR);
                        // Reprogramar y vincular un envío son cambios de la operación. Van
                        // aquí, explícitos, para no abrir cualquier PUT futuro.
                        auth.requestMatchers(HttpMethod.PUT, "/api/pickups/*/schedule", "/api/pickups/*/shipment")
                                .hasRole(SecurityProperties.ROL_OPERADOR);
                        // El comprobante lleva nombres de personas (HU-14): no se abre al
                        // público como /api/tracking, aunque sea una consulta.
                        auth.requestMatchers(HttpMethod.GET, "/api/shipments/*/receipt")
                                .hasRole(SecurityProperties.ROL_OPERADOR);
                        // Lo que no esté listado arriba queda cerrado: un endpoint nuevo no
                        // puede quedar público por olvido.
                        auth.anyRequest().denyAll();
                    } else {
                        auth.anyRequest().permitAll();
                    }
                })
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDeRoles())));

        if (protegido) {
            log.info("Seguridad activa: la escritura requiere token de operador (POST /api/auth/login)");
            if (!propiedades.adminConfigurado()) {
                log.warn("Sin clave de administrador: /api/admin/** queda inaccesible. "
                        + "Defina TRACKFLOW_ADMIN_CLAVE si necesita reconstruir proyecciones.");
            }
        } else {
            log.warn("Sin clave de operador configurada: los endpoints de escritura están abiertos. "
                    + "Defina TRACKFLOW_OPERADOR_CLAVE en cualquier entorno accesible desde internet.");
        }

        return http.build();
    }

    /**
     * El administrador contiene a los otros dos: puede lo que el operador y lo que el
     * supervisor.
     *
     * Y solo el: supervisor y operador no son dos escalones de una misma escalera, sino
     * dos funciones distintas. Quien supervisa consulta reportes pero no registra
     * envios (HU-11), asi que encadenarlos le habria dado permiso de escritura sin que
     * nadie se lo diera.
     *
     * Sin esta jerarquia cada regla exigia su rol literal, asi que al administrador
     * habia que darle los tres para que pudiera registrar un envio o abrir un reporte.
     * Funcionaba, pero obligaba a recordar la lista completa cada vez que se creaba una
     * cuenta, y bastaba olvidar uno para dejar a alguien a medias.
     *
     * Se declara en un solo sitio: las reglas de abajo siguen pidiendo el rol minimo
     * que corresponde a cada ruta, que es como se leen mejor.
     */
    @Bean
    static RoleHierarchy jerarquiaDeRoles() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role(SecurityProperties.ROL_ADMIN)
                .implies(SecurityProperties.ROL_SUPERVISOR, SecurityProperties.ROL_OPERADOR)
                .build();
    }

    /** Traduce el claim "roles" del token a las autoridades que espera Spring Security. */
    private JwtAuthenticationConverter conversorDeRoles() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("roles");
        autoridades.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(autoridades);
        return conversor;
    }
}
