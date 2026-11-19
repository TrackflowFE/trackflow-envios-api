package com.trackflow.modules.chatbot.application;

import com.trackflow.shared.seguridad.VentanasDeIntentos;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Las conversaciones abiertas y el tope de mensajes de cada una.
 *
 * Viven solo en memoria y sin ningún dato de la persona: una sesión es un número al
 * azar. Si la aplicación se reinicia, las sesiones se pierden y el cliente crea otra.
 * El tope por sesión no basta por sí solo (alguien puede inventar sesiones), por eso
 * hay además un tope por IP en el filtro.
 */
@Component
public class SesionesDeChat {

    private static final int CAPACIDAD = 10000;
    private static final long VIDA_MILLIS = 2 * 60 * 60 * 1000L;

    private final Map<UUID, Long> sesiones = new LinkedHashMap<>(16, 0.75f, true);
    private final VentanasDeIntentos mensajes;
    private final int limiteMensajes;
    private final long segundos;
    private final Clock clock;

    public SesionesDeChat(@Value("${trackflow.chatbot.limite.mensajes-por-minuto:10}") int limiteMensajes,
            @Value("${trackflow.chatbot.limite.ventana-segundos:60}") long segundos, Clock clock) {
        if (limiteMensajes < 1 || segundos < 1 || segundos > 86400) {
            throw new IllegalArgumentException(
                    "El límite del chatbot debe ser positivo y la ventana de hasta 86400 segundos");
        }
        this.limiteMensajes = limiteMensajes;
        this.segundos = segundos;
        this.clock = clock;
        this.mensajes = new VentanasDeIntentos(CAPACIDAD, segundos * 1000, clock);
    }

    public synchronized UUID crear() {
        long ahora = clock.millis();
        sesiones.values().removeIf(vence -> vence <= ahora);
        if (sesiones.size() >= CAPACIDAD) {
            sesiones.remove(sesiones.keySet().iterator().next());
        }
        UUID id = UUID.randomUUID();
        sesiones.put(id, ahora + VIDA_MILLIS);
        return id;
    }

    /** Comprueba que la sesión exista y cuenta el mensaje; si se pasó del tope, lo rechaza. */
    public synchronized void admitir(UUID id) {
        long ahora = clock.millis();
        Long vence = id == null ? null : sesiones.get(id);
        if (vence == null || vence <= ahora) {
            sesiones.remove(id);
            throw new SesionInvalidaException();
        }
        sesiones.put(id, ahora + VIDA_MILLIS);
        if (mensajes.reservar(id.toString(), limiteMensajes) == null) {
            throw new DemasiadosMensajesException(segundos);
        }
    }
}
