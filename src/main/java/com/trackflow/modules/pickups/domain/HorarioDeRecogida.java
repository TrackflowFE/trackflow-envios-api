package com.trackflow.modules.pickups.domain;

import java.time.Duration;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/**
 * Cuándo se puede recoger: los días de atención, el horario del día, las franjas
 * disponibles y con cuánta antelación hay que avisar.
 *
 * Es configuración, no código: los valores vienen del entorno (ver
 * HorarioDeRecogidaConfig) para que cambiarlos no exija recompilar ni desplegar.
 *
 * Las franjas se nombran "08:00-10:00": la primera hora es el inicio y la segunda el
 * fin. El inicio es lo que se compara con la antelación mínima, porque es cuando el
 * operador sale.
 */
public final class HorarioDeRecogida {

    private final ZoneId zona;
    private final Set<DayOfWeek> diasOperativos;
    private final List<String> franjas;
    private final Duration anticipacionMinima;
    private final int horizonteDias;

    public HorarioDeRecogida(ZoneId zona, Set<DayOfWeek> diasOperativos, List<String> franjas,
            Duration anticipacionMinima, int horizonteDias) {
        this.zona = zona;
        this.diasOperativos = Set.copyOf(diasOperativos);
        this.franjas = List.copyOf(franjas);
        this.anticipacionMinima = anticipacionMinima;
        this.horizonteDias = horizonteDias;
    }

    /**
     * Revisa que la fecha y la franja se puedan atender, y dice por qué no si no.
     *
     * Se revisa en este orden porque así el motivo que se le da a quien pide la
     * recogida es el más cercano al problema: una fecha de ayer no se rechaza por
     * domingo, sino por estar en el pasado.
     */
    public void exigir(LocalDate fecha, String franja, Instant ahora) {
        LocalDate hoy = hoy(ahora);

        if (fecha.isBefore(hoy)) {
            throw new FechaDeRecogidaEnElPasadoException(fecha, hoy);
        }
        if (!diasOperativos.contains(fecha.getDayOfWeek())) {
            throw new DiaNoOperativoException(fecha);
        }
        if (fecha.isAfter(hoy.plusDays(horizonteDias))) {
            throw new FechaFueraDelHorizonteException(fecha, horizonteDias);
        }
        if (!franjas.contains(franja)) {
            throw new FranjaDesconocidaException(franja, franjas);
        }
        if (inicioDe(franja, fecha).isBefore(ahora.plus(anticipacionMinima))) {
            throw new SinAnticipacionSuficienteException(franja, anticipacionMinima);
        }
    }

    /** El día de hoy en la zona donde se recoge, no en UTC. */
    public LocalDate hoy(Instant ahora) {
        return ahora.atZone(zona).toLocalDate();
    }

    /** La hora en que empieza la franja, en la zona de atención. */
    public Instant inicioDe(String franja, LocalDate fecha) {
        return fecha.atTime(inicioDe(franja)).atZone(zona).toInstant();
    }

    private LocalTime inicioDe(String franja) {
        return LocalTime.parse(franja.substring(0, 5));
    }

    public ZoneId zona() {
        return zona;
    }

    public Set<DayOfWeek> diasOperativos() {
        return diasOperativos;
    }

    public List<String> franjas() {
        return franjas;
    }

    public Duration anticipacionMinima() {
        return anticipacionMinima;
    }

    public int horizonteDias() {
        return horizonteDias;
    }
}
