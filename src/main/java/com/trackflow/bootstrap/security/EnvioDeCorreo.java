package com.trackflow.bootstrap.security;

/**
 * Enviar un correo. La aplicación solo conoce esta interfaz: que detrás haya un
 * servidor SMTP o la bitacora del arranque no cambia nada de la lógica (HU-10).
 */
public interface EnvioDeCorreo {

    void enviar(String destino, String asunto, String cuerpo);
}
