package com.trackflow.modules.chatbot.application;

import com.trackflow.modules.chatbot.domain.Normalizador;
import com.trackflow.modules.chatbot.domain.PreguntaFrecuente;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Las preguntas frecuentes activas, en memoria, y la forma de elegir cuál contesta.
 *
 * Elegir es contar coincidencias de palabras, sin inteligencia artificial: cada palabra
 * del mensaje vale 2 si es palabra clave de la pregunta, 1 si aparece en su texto y 0
 * si no. El puntaje es lo que suman, dividido entre lo máximo posible (2 por palabra).
 * Si el mejor no llega al umbral, el chatbot dice que no sabe. Es determinista: el
 * mismo mensaje con el mismo contenido da siempre la misma respuesta.
 *
 * Se recarga cada vez que un administrador cambia el contenido.
 */
@Component
public class CatalogoDeFaq {

    public record Entrada(Long id, String intencion, String categoria, String pregunta, String respuesta, int orden,
            List<Set<String>> claves, Set<String> palabrasPregunta) {

        static Entrada de(PreguntaFrecuente faq) {
            List<Set<String>> claves = faq.palabrasClave().stream().map(Normalizador::palabras)
                    .filter(palabras -> !palabras.isEmpty()).toList();
            return new Entrada(faq.id(), faq.intencion(), faq.categoria(), faq.pregunta(), faq.respuesta(),
                    faq.orden(), claves, Normalizador.palabras(faq.pregunta()));
        }

        double puntaje(Set<String> mensaje) {
            Set<String> deClave = new HashSet<>();
            claves.stream().filter(mensaje::containsAll).forEach(deClave::addAll);
            int suma = 0;
            for (String palabra : mensaje) {
                suma += deClave.contains(palabra) ? 2 : palabrasPregunta.contains(palabra) ? 1 : 0;
            }
            return Math.min(1.0, suma / (2.0 * mensaje.size()));
        }
    }

    public record Coincidencia(Entrada entrada, double puntaje) {
    }

    private final FaqRepository faqs;
    private final double umbral;
    private final AtomicReference<List<Entrada>> entradas = new AtomicReference<>(List.of());

    public CatalogoDeFaq(FaqRepository faqs, @Value("${trackflow.chatbot.umbral:0.35}") double umbral) {
        if (umbral <= 0 || umbral > 1) {
            throw new IllegalArgumentException("El umbral del chatbot debe estar entre 0 (sin incluir) y 1");
        }
        this.faqs = faqs;
        this.umbral = umbral;
    }

    public void recargar() {
        entradas.set(faqs.activas().stream().map(Entrada::de).toList());
    }

    public Optional<Coincidencia> mejor(String texto) {
        Set<String> mensaje = Normalizador.palabras(texto);
        if (mensaje.isEmpty()) {
            return Optional.empty();
        }
        return entradas.get().stream().map(entrada -> new Coincidencia(entrada, entrada.puntaje(mensaje)))
                .filter(c -> c.puntaje() >= umbral)
                .min(Comparator.comparingDouble((Coincidencia c) -> -c.puntaje())
                        .thenComparingInt(c -> c.entrada().orden())
                        .thenComparing(c -> c.entrada().id(), Comparator.<Long>nullsLast(Comparator.<Long>naturalOrder())));
    }

    /** Preguntas para mostrar como botones: las primeras por orden, sin el saludo. */
    public List<Entrada> sugerencias(int maximo) {
        return entradas.get().stream().filter(e -> !"general".equals(e.categoria())).limit(maximo).toList();
    }
}
