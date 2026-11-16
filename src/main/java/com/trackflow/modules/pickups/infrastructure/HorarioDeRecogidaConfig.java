package com.trackflow.modules.pickups.infrastructure;

import com.trackflow.modules.pickups.domain.HorarioDeRecogida;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Arma el horario de atención a partir del entorno.
 *
 * Cambiar los días, las franjas o la antelación no exige recompilar: se cambia la
 * variable y se reinicia. Por eso los valores vienen con su valor por defecto en la
 * propia anotación y también en application.properties.
 */
@Configuration
class HorarioDeRecogidaConfig {

    @Bean
    HorarioDeRecogida horarioDeRecogida(
            @Value("${trackflow.pickups.zona-horaria:America/Bogota}") String zona,
            @Value("${trackflow.pickups.dias-operativos:1,2,3,4,5,6}") String diasOperativos,
            @Value("${trackflow.pickups.franjas:08:00-10:00,10:00-12:00,12:00-14:00,14:00-16:00,16:00-18:00}")
            String franjas,
            @Value("${trackflow.pickups.anticipacion-minima-horas:4}") long anticipacionHoras,
            @Value("${trackflow.pickups.horizonte-maximo-dias:30}") int horizonteDias) {
        return new HorarioDeRecogida(
                ZoneId.of(zona),
                diasDeLaSemana(diasOperativos),
                List.of(franjas.split(",")),
                Duration.ofHours(anticipacionHoras),
                horizonteDias);
    }

    /** Los días van en su número de ISO: 1 es lunes y 7 es domingo. */
    private static Set<DayOfWeek> diasDeLaSemana(String configuracion) {
        return Arrays.stream(configuracion.split(","))
                .map(String::trim)
                .filter(dia -> !dia.isEmpty())
                .map(Integer::parseInt)
                .map(DayOfWeek::of)
                .collect(Collectors.toUnmodifiableSet());
    }
}
