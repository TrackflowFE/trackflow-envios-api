package com.trackflow.modules.pickups.infrastructure;

import com.trackflow.modules.pickups.application.OperadoresDisponibles;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operadores activos, leídos de la tabla de usuarios.
 *
 * Los roles van separados por espacio, como viajan en el claim del token: "OPERADOR"
 * o "ADMIN OPERADOR". La búsqueda rodea la cadena con espacios para que "OPERADOR"
 * no coincida con un rol que lo contenga como parte de otro nombre.
 */
@Component
class OperadoresJpa implements OperadoresDisponibles {

    private static final String OPERADORES = """
            select username
            from auth_users
            where active = true
              and (' ' || roles || ' ') like '% OPERADOR %'
            order by username
            """;

    private final JdbcTemplate jdbc;

    OperadoresJpa(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> activos() {
        return jdbc.queryForList(OPERADORES, String.class);
    }
}
