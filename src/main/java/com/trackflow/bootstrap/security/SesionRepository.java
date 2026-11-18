package com.trackflow.bootstrap.security;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SesionRepository extends JpaRepository<Sesion, String> {

    /**
     * Cierra de golpe las sesiones abiertas de un usuario (HU-11): al desactivarlo o
     * cambiarle el rol, sus tokens dejan de servir en las tres APIs, porque todas
     * consultan esta misma tabla.
     *
     * @param excepto jti que se conserva, para no echar a quien está haciendo el cambio
     */
    @Modifying
    @Query("update Sesion s set s.closedAt = :ahora where s.username = :username "
            + "and s.closedAt is null and (:excepto is null or s.jti <> :excepto)")
    int cerrarAbiertasDe(String username, Instant ahora, String excepto);

    /** Las vencidas ya las rechaza la validación de fecha del token; guardarlas no aporta. */
    @Modifying
    @Query("delete from Sesion s where s.expiresAt < :ahora")
    int borrarVencidas(Instant ahora);
}
