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

/** Un cambio en la vida de una PQR: quién, cuándo y de qué estado a cuál. */
@Entity
@Table(name = "pqr_eventos")
public class PqrEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String radicado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private EstadoPqr estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoPqr estadoNuevo;

    @Column(length = 500)
    private String detalle;

    @Column(nullable = false, length = 50)
    private String actor;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected PqrEvento() {
    }

    public PqrEvento(String radicado, EstadoPqr anterior, EstadoPqr nuevo, String detalle, String actor,
            Instant occurredAt) {
        this.radicado = radicado;
        this.estadoAnterior = anterior;
        this.estadoNuevo = nuevo;
        this.detalle = detalle;
        this.actor = actor;
        this.occurredAt = occurredAt;
    }

    public String radicado() {
        return radicado;
    }

    public EstadoPqr estadoAnterior() {
        return estadoAnterior;
    }

    public EstadoPqr estadoNuevo() {
        return estadoNuevo;
    }

    public String detalle() {
        return detalle;
    }

    public String actor() {
        return actor;
    }

    public Instant occurredAt() {
        return occurredAt;
    }
}
