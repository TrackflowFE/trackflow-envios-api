package com.trackflow.bootstrap.security;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SolicitudDeClaveRepository extends JpaRepository<SolicitudDeClave, String> {

    /**
     * Al pedir un enlace nuevo, los anteriores del mismo usuario dejan de servir: si
     * no, cada solicitud sumaría una puerta abierta durante media hora.
     */
    @Modifying
    @Query("delete from SolicitudDeClave s where s.username = :username and s.usedAt is null")
    int borrarPendientesDe(String username);

    /** Las vencidas ya no sirven para nada; guardarlas solo hace crecer la tabla. */
    @Modifying
    @Query("delete from SolicitudDeClave s where s.expiresAt < :ahora")
    int borrarVencidas(Instant ahora);
}
