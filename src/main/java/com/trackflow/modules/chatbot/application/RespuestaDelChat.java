package com.trackflow.modules.chatbot.application;

import java.util.List;

/**
 * Lo que el chatbot contesta. Las acciones son códigos, no direcciones: la pantalla
 * decide a dónde navegar.
 */
public record RespuestaDelChat(Resultado resultado, String intencion, String texto, List<Accion> acciones) {

    public enum Resultado { RESPUESTA, FUERA_DE_TEMA, NUMERO_DE_SEGUIMIENTO }

    public enum TipoAccion { ABRIR_PQR, IR_A_CONSULTA }

    public record Accion(TipoAccion tipo, String etiqueta) {
    }

    static final Accion RADICAR_PQR = new Accion(TipoAccion.ABRIR_PQR, "Radicar una PQR");
    static final Accion RASTREAR = new Accion(TipoAccion.IR_A_CONSULTA, "Rastrear mi envío");

    static RespuestaDelChat fueraDeTema() {
        return new RespuestaDelChat(Resultado.FUERA_DE_TEMA, "fuera_de_tema",
                "Eso está fuera de lo que puedo responder. ¿Quieres radicar una PQR para que una persona te ayude?",
                List.of(RADICAR_PQR));
    }

    /** No repite el número recibido y no dice nada sobre ningún envío. */
    static RespuestaDelChat numeroDeSeguimiento() {
        return new RespuestaDelChat(Resultado.NUMERO_DE_SEGUIMIENTO, "numero_de_seguimiento",
                "Por tu seguridad no consulto ni muestro información de envíos en este chat. "
                        + "Para ver el estado de tu envío usa la página oficial de rastreo.",
                List.of(RASTREAR));
    }

    static RespuestaDelChat de(CatalogoDeFaq.Entrada faq) {
        List<Accion> acciones = switch (faq.categoria()) {
            case "pqr" -> List.of(RADICAR_PQR);
            case "seguimiento" -> List.of(RASTREAR);
            default -> List.of();
        };
        return new RespuestaDelChat(Resultado.RESPUESTA, faq.intencion(), faq.respuesta(), acciones);
    }
}
