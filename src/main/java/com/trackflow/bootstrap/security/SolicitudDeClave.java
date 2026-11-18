package com.trackflow.bootstrap.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Un enlace de recuperación pendiente (HU-10).
 *
 * De la clave de un solo uso que viaja por correo no se guarda el valor, solo su
 * huella SHA-256: quien pudiera leer esta tabla no obtendría ningún enlace utilizable.
 * El uso único se materializa en usedAt, y la vigencia de 30 minutos en expiresAt.
 */
@Entity
@Table(name = "auth_password_resets")
public class SolicitudDeClave {

    @Id
    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    protected SolicitudDeClave() {
    }

    public SolicitudDeClave(String tokenHash, String username, Instant createdAt, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.username = username;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getUsername() {
        return username;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean usada() {
        return usedAt != null;
    }

    public boolean vencida(Instant ahora) {
        return !expiresAt.isAfter(ahora);
    }

    void marcarUsada(Instant ahora) {
        this.usedAt = ahora;
    }
}
