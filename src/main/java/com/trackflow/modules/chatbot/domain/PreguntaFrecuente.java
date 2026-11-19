package com.trackflow.modules.chatbot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Una pregunta frecuente con su respuesta, editable por un administrador.
 *
 * Nunca se borra: se desactiva. La respuesta es texto plano; quien la muestra no debe
 * interpretarla como HTML.
 */
@Entity
@Table(name = "chatbot_faq")
public class PreguntaFrecuente {

    private static final Pattern INTENCION = Pattern.compile("[a-z][a-z0-9_]{2,59}");
    private static final int MAXIMO_PALABRAS = 15;
    private static final int MAXIMO_LARGO_PALABRA = 30;
    private static final String SEPARADOR = ",";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60, updatable = false)
    private String intencion;

    @Column(nullable = false, length = 40)
    private String categoria;

    @Column(nullable = false, length = 200)
    private String pregunta;

    @Column(name = "palabras_clave", nullable = false, length = 500)
    private String palabrasClave;

    @Column(nullable = false, length = 1000)
    private String respuesta;

    @Column(nullable = false)
    private boolean activo;

    @Column(nullable = false)
    private int orden;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    protected PreguntaFrecuente() {
    }

    /** Lo que un administrador puede cambiar de una pregunta, ya validado. */
    public record Contenido(String categoria, String pregunta, List<String> palabrasClave, String respuesta,
            int orden) {

        public Contenido {
            categoria = texto("La categoría", categoria, 2, 40).toLowerCase(Locale.ROOT);
            pregunta = texto("La pregunta", pregunta, 5, 200);
            respuesta = texto("La respuesta", respuesta, 10, 1000);
            palabrasClave = limpiarPalabras(palabrasClave);
            if (orden < 0 || orden > 999) {
                throw new IllegalArgumentException("El orden debe estar entre 0 y 999.");
            }
        }
    }

    public static PreguntaFrecuente crear(String intencion, Contenido contenido, String actor, Instant ahora) {
        if (intencion == null || !INTENCION.matcher(intencion).matches()) {
            throw new IllegalArgumentException(
                    "La intención usa minúsculas, números y guion bajo (3 a 60 caracteres) y empieza con letra.");
        }
        PreguntaFrecuente faq = new PreguntaFrecuente();
        faq.intencion = intencion;
        faq.activo = true;
        faq.reemplazar(contenido, actor, ahora);
        return faq;
    }

    public void editar(Contenido contenido, String actor, Instant ahora) {
        reemplazar(contenido, actor, ahora);
    }

    public void cambiarActivo(boolean activo, String actor, Instant ahora) {
        this.activo = activo;
        this.updatedAt = ahora;
        this.updatedBy = actor;
    }

    private void reemplazar(Contenido contenido, String actor, Instant ahora) {
        this.categoria = contenido.categoria();
        this.pregunta = contenido.pregunta();
        this.palabrasClave = String.join(SEPARADOR, contenido.palabrasClave());
        this.respuesta = contenido.respuesta();
        this.orden = contenido.orden();
        this.updatedAt = ahora;
        this.updatedBy = actor;
    }

    private static String texto(String nombre, String valor, int minimo, int maximo) {
        String limpio = valor == null ? "" : valor.strip();
        if (limpio.length() < minimo || limpio.length() > maximo) {
            throw new IllegalArgumentException(nombre + " debe tener entre " + minimo + " y " + maximo + " caracteres.");
        }
        if (limpio.chars().anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\t')) {
            throw new IllegalArgumentException(nombre + " no puede tener caracteres de control.");
        }
        return limpio;
    }

    private static List<String> limpiarPalabras(List<String> palabras) {
        Set<String> limpias = new LinkedHashSet<>();
        for (String palabra : palabras == null ? List.<String>of() : palabras) {
            String normal = Normalizador.normalizar(palabra);
            if (normal.length() > MAXIMO_LARGO_PALABRA) {
                throw new IllegalArgumentException("Cada palabra clave admite hasta " + MAXIMO_LARGO_PALABRA
                        + " caracteres.");
            }
            if (!normal.isEmpty()) {
                limpias.add(normal);
            }
        }
        if (limpias.isEmpty() || limpias.size() > MAXIMO_PALABRAS) {
            throw new IllegalArgumentException("Indica entre 1 y " + MAXIMO_PALABRAS + " palabras clave.");
        }
        return List.copyOf(limpias);
    }

    public Long id() {
        return id;
    }

    public String intencion() {
        return intencion;
    }

    public String categoria() {
        return categoria;
    }

    public String pregunta() {
        return pregunta;
    }

    public List<String> palabrasClave() {
        return Arrays.asList(palabrasClave.split(SEPARADOR));
    }

    public String respuesta() {
        return respuesta;
    }

    public boolean activo() {
        return activo;
    }

    public int orden() {
        return orden;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public String updatedBy() {
        return updatedBy;
    }
}
