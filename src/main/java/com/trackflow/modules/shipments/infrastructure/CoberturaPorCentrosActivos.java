package com.trackflow.modules.shipments.infrastructure;

import com.trackflow.shared.geografia.CoberturaLogistica;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cobertura vista desde la API de envíos. Una ciudad tiene cobertura si tiene al menos
 * un centro activo, y dos ciudades tienen ruta si la red de tramos activos las conecta.
 *
 * Las tablas logistics_centers y logistics_route_legs son de la API de logística; aquí
 * solo se leen, como lecturas de referencia declaradas. Igual que en logística, un tramo
 * solo sirve si las dos ciudades que une tienen un centro activo.
 */
@Component
class CoberturaPorCentrosActivos implements CoberturaLogistica {

    private static final String TRAMOS_ACTIVOS = """
            select l.from_city_id, l.to_city_id
            from logistics_route_legs l
            where l.active = true
              and exists (select 1 from logistics_centers c where c.city_id = l.from_city_id and c.active = true)
              and exists (select 1 from logistics_centers c where c.city_id = l.to_city_id and c.active = true)
            """;

    private final JdbcTemplate jdbc;

    CoberturaPorCentrosActivos(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tieneCobertura(Long ciudadId) {
        if (ciudadId == null) {
            return false;
        }
        Boolean existe = jdbc.queryForObject(
                "select exists(select 1 from logistics_centers where city_id = ? and active = true)",
                Boolean.class, ciudadId);
        return Boolean.TRUE.equals(existe);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> ciudadesConCobertura() {
        return new HashSet<>(jdbc.queryForList(
                "select distinct city_id from logistics_centers where active = true", Long.class));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hayRuta(Long origenId, Long destinoId) {
        if (origenId == null || destinoId == null) {
            return false;
        }
        Map<Long, List<Long>> salidas = new HashMap<>();
        RowCallbackHandler cargar = rs -> salidas
                .computeIfAbsent(rs.getLong("from_city_id"), id -> new ArrayList<>())
                .add(rs.getLong("to_city_id"));
        jdbc.query(TRAMOS_ACTIVOS, cargar);
        return alcanzable(salidas, origenId, destinoId);
    }

    /** Si desde una ciudad se llega a la otra siguiendo los tramos en el sentido en que se recorren. */
    static boolean alcanzable(Map<Long, List<Long>> salidas, Long origen, Long destino) {
        if (origen.equals(destino)) {
            return true;
        }
        Set<Long> visitadas = new HashSet<>(Set.of(origen));
        Deque<Long> porVisitar = new ArrayDeque<>(List.of(origen));
        while (!porVisitar.isEmpty()) {
            Long actual = porVisitar.poll();
            for (Long siguiente : salidas.getOrDefault(actual, List.of())) {
                if (siguiente.equals(destino)) {
                    return true;
                }
                if (visitadas.add(siguiente)) {
                    porVisitar.add(siguiente);
                }
            }
        }
        return false;
    }
}
