package com.trackflow.bootstrap.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;

/**
 * Usuario del sistema (HU-08). Solo guarda el hash BCrypt de la contraseña, nunca
 * la contraseña: BCrypt incluye la sal en el propio hash, así que dos usuarios con
 * la misma clave tienen hashes distintos.
 *
 * Crear, editar y desactivar usuarios desde la API es la HU-12; por ahora se crean
 * al arrancar (ver UsuariosIniciales).
 */
@Entity
@Table(name = "auth_users")
public class Usuario {

    @Id
    @Column(length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String roles;

    /**
     * Correo de contacto, opcional: es por donde viaja el enlace de recuperacion
     * (HU-10). Quien no lo tenga definido no puede recuperar la clave por correo y
     * depende de que un administrador se la cambie.
     */
    @Column(length = 120)
    private String email;

    /**
     * Cierto cuando la contrasena la fijo su dueno (cambio o recuperacion, HU-10).
     * Desde ese momento el arranque ya no la sobreescribe con la del entorno.
     */
    @Column(name = "password_set_by_user", nullable = false)
    private boolean passwordSetByUser;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Intentos fallidos seguidos; vuelve a cero al entrar bien (HU-11). */
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    /** Mientras sea futura, no puede entrar aunque acierte la clave. */
    @Column(name = "locked_until")
    private Instant lockedUntil;

    protected Usuario() {
    }

    public Usuario(String username, String passwordHash, String roles, boolean active, Instant createdAt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.roles = roles;
        this.active = active;
        this.createdAt = createdAt;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRoles() {
        return roles;
    }

    public String getEmail() {
        return email;
    }

    public boolean isPasswordSetByUser() {
        return passwordSetByUser;
    }

    public boolean isActive() {
        return active;
    }

    public boolean bloqueado(Instant ahora) {
        return lockedUntil != null && lockedUntil.isAfter(ahora);
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    /**
     * Suma un intento fallido y bloquea la cuenta al llegar al tope.
     *
     * @return true si este intento dejó la cuenta bloqueada
     */
    public boolean registrarIntentoFallido(int tope, Duration bloqueo, Instant ahora) {
        failedAttempts++;
        if (failedAttempts < tope) {
            return false;
        }
        lockedUntil = ahora.plus(bloqueo);
        return true;
    }

    /** Tras entrar bien: el contador solo cuenta fallos seguidos, no acumulados. */
    public void registrarIngresoCorrecto() {
        failedAttempts = 0;
        lockedUntil = null;
    }

    public void desbloquear() {
        failedAttempts = 0;
        lockedUntil = null;
    }

    public void cambiarRoles(String roles) {
        this.roles = roles;
    }

    public void cambiarActivo(boolean active) {
        this.active = active;
    }

    void cambiarPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Cambia la clave por decision de su dueno: ademas de guardar el nuevo hash,
     * deja la cuenta fuera del alcance de las claves del entorno y la desbloquea,
     * porque quien acaba de demostrar que es el dueno no debe seguir bloqueado por
     * intentos anteriores.
     */
    void cambiarClavePropia(String passwordHash) {
        this.passwordHash = passwordHash;
        this.passwordSetByUser = true;
        desbloquear();
    }

    void cambiarEmail(String email) {
        this.email = email;
    }
}
