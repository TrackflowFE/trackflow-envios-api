package com.trackflow.bootstrap.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Correo de contacto de las tres cuentas que se crean al arrancar (HU-10).
 *
 * Van por variables de entorno, igual que sus claves, porque son cuentas fijas de la
 * operación: pedirle al administrador que los registre uno por uno tras cada
 * despliegue sería un paso manual fácil de olvidar, y olvidarlo deja a todos sin
 * poder recuperar su contraseña.
 */
@Component
public class CorreosIniciales {

    private final String operador;
    private final String supervisor;
    private final String admin;

    public CorreosIniciales(
            @Value("${trackflow.security.operador-email:}") String operador,
            @Value("${trackflow.security.supervisor-email:}") String supervisor,
            @Value("${trackflow.security.admin-email:}") String admin) {
        this.operador = operador;
        this.supervisor = supervisor;
        this.admin = admin;
    }

    public String operador() {
        return operador;
    }

    public String supervisor() {
        return supervisor;
    }

    public String admin() {
        return admin;
    }
}
