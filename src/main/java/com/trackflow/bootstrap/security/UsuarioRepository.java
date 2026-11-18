package com.trackflow.bootstrap.security;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    /** Para recuperar la clave (HU-10): la persona recuerda su correo, no su usuario. */
    Optional<Usuario> findByEmailIgnoreCase(String email);
}
