package com.trackflow.modules.pqr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;

/**
 * Una petición, queja o reclamo.
 *
 * Es un agregado propio: no depende del envío, que a veces ni se menciona. Los datos
 * de contacto no salen de este módulo salvo para avisar al solicitante: la consulta
 * pública nunca los devuelve.
 */
@Entity
@Table(name = "pqr")
public class Pqr {

    private static final int RESPUESTA_MINIMA = 10;
    private static final int RESPUESTA_MAXIMA = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String radicado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPqr tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPqr estado;

    @Column(nullable = false, length = 2000)
    private String descripcion;

    @Column(name = "contacto_nombre", nullable = false, length = 120)
    private String contactoNombre;

    @Column(name = "contacto_email", nullable = false, length = 120)
    private String contactoEmail;

    @Column(name = "contacto_telefono", length = 20)
    private String contactoTelefono;

    @Column(name = "numero_seguimiento", length = 14)
    private String numeroSeguimiento;

    @Column(name = "acepta_tratamiento_datos", nullable = false)
    private boolean aceptaTratamientoDatos;

    @Column(length = 2000)
    private String respuesta;

    @Column(name = "respondido_por", length = 50)
    private String respondidoPor;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Pqr() {
    }

    private Pqr(String radicado, TipoPqr tipo, String descripcion, Contacto contacto, String numeroSeguimiento,
            Instant ahora) {
        this.radicado = radicado;
        this.tipo = tipo;
        this.estado = EstadoPqr.RECIBIDA;
        this.descripcion = descripcion;
        this.contactoNombre = contacto.nombre();
        this.contactoEmail = contacto.email().strip().toLowerCase(Locale.ROOT);
        this.contactoTelefono = contacto.telefono();
        this.numeroSeguimiento = numeroSeguimiento;
        this.aceptaTratamientoDatos = true;
        this.createdAt = ahora;
        this.updatedAt = ahora;
    }

    /** Quién radica y cómo se le avisa. El teléfono es opcional. */
    public record Contacto(String nombre, String email, String telefono) {
    }

    /**
     * Radica una PQR nueva. Sin la aceptación del tratamiento de datos no hay PQR: la
     * Ley 1581 pide que sea previa.
     */
    public static Pqr radicar(String radicado, TipoPqr tipo, String descripcion, Contacto contacto,
            String numeroSeguimiento, boolean aceptaTratamientoDatos, Instant ahora) {
        if (!aceptaTratamientoDatos) {
            throw new IllegalArgumentException("Debe aceptar el tratamiento de datos personales");
        }
        return new Pqr(radicado, tipo, descripcion, contacto, numeroSeguimiento, ahora);
    }

    /**
     * Pasa la PQR al estado pedido. Resolver exige respuesta; el resto de estados la
     * ignora, y reabrir (volver a EN_TRAMITE) conserva la anterior hasta que haya otra.
     */
    public PqrEvento cambiarEstado(EstadoPqr destino, String textoRespuesta, String actor, Instant ahora) {
        if (!estado.puedePasarA(destino)) {
            throw new TransicionDePqrInvalidaException(radicado, estado, destino);
        }
        if (destino == EstadoPqr.RESUELTA) {
            this.respuesta = respuestaValida(textoRespuesta);
            this.respondidoPor = actor;
        }
        EstadoPqr anterior = this.estado;
        this.estado = destino;
        this.updatedAt = ahora;
        return new PqrEvento(radicado, anterior, destino, null, actor, ahora);
    }

    /** El evento de la radicación, que no tiene estado anterior. */
    public PqrEvento eventoDeRadicacion() {
        return new PqrEvento(radicado, null, EstadoPqr.RECIBIDA, null, "solicitante", createdAt);
    }

    public boolean esDe(String email) {
        return email != null && contactoEmail.equals(email.strip().toLowerCase(Locale.ROOT));
    }

    private static String respuestaValida(String texto) {
        String limpio = texto == null ? "" : texto.strip();
        if (limpio.length() < RESPUESTA_MINIMA || limpio.length() > RESPUESTA_MAXIMA) {
            throw new RespuestaRequeridaException();
        }
        return limpio;
    }

    public String radicado() {
        return radicado;
    }

    public TipoPqr tipo() {
        return tipo;
    }

    public EstadoPqr estado() {
        return estado;
    }

    public String descripcion() {
        return descripcion;
    }

    public String contactoNombre() {
        return contactoNombre;
    }

    public String contactoEmail() {
        return contactoEmail;
    }

    public String contactoTelefono() {
        return contactoTelefono;
    }

    public String numeroSeguimiento() {
        return numeroSeguimiento;
    }

    public String respuesta() {
        return respuesta;
    }

    public String respondidoPor() {
        return respondidoPor;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
