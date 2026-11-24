package com.trackflow.bootstrap.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Registro de un cambio sobre usuarios, roles o permisos (HU-11). Solo se escribe:
 * no hay forma de editarlo ni de borrarlo desde la aplicación, porque su utilidad
 * depende de que nadie pueda retocarlo después.
 */
@Entity
@Table(name = "auth_audit")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, updatable = false)
    private String actor;

    @Column(nullable = false, length = 40, updatable = false)
    private String accion;

    @Column(length = 100, updatable = false)
    private String objetivo;

    @Column(length = 500, updatable = false)
    private String detalle;

    @Column(name = "ocurrido_en", nullable = false, updatable = false)
    private Instant ocurridoEn;

    /**
     * Desde dónde se hizo. Nulo cuando no hay petición detrás: el arranque de la
     * aplicación, o una tarea asíncrona. Cabe una IPv6 con zona.
     */
    @Column(name = "ip_origen", length = 45, updatable = false)
    private String ipOrigen;

    protected Auditoria() {
    }

    public Auditoria(String actor, String accion, String objetivo, String detalle, Instant ocurridoEn) {
        this(actor, accion, objetivo, detalle, ocurridoEn, null);
    }

    public Auditoria(String actor, String accion, String objetivo, String detalle, Instant ocurridoEn,
            String ipOrigen) {
        this.ipOrigen = ipOrigen;
        this.actor = actor;
        this.accion = accion;
        this.objetivo = objetivo;
        this.detalle = detalle;
        this.ocurridoEn = ocurridoEn;
    }

    public Long getId() {
        return id;
    }

    public String getActor() {
        return actor;
    }

    public String getAccion() {
        return accion;
    }

    public String getObjetivo() {
        return objetivo;
    }

    public String getDetalle() {
        return detalle;
    }

    public Instant getOcurridoEn() {
        return ocurridoEn;
    }

    public String getIpOrigen() {
        return ipOrigen;
    }
}
