package com.trackflow.modules.pickups.api.dto;

import com.trackflow.modules.pickups.domain.HorarioDeRecogida;
import java.util.List;

/**
 * Cuándo se puede recoger.
 *
 * Es público a propósito: es el horario del servicio, no información de ninguna
 * recogida. Quien va a pedir una visita necesita verlo para no proponer una franja
 * que luego se vaya a rechazar.
 */
public record HorarioRecogidaResponse(
        List<Integer> days,
        String open,
        String close,
        List<String> slots,
        long leadTimeHours,
        int horizonDays,
        String timeZone) {

    public static HorarioRecogidaResponse from(HorarioDeRecogida horario) {
        List<String> franjas = horario.franjas();

        return new HorarioRecogidaResponse(
                horario.diasOperativos().stream().map(Enum::ordinal).map(ordinal -> ordinal + 1).sorted().toList(),
                franjas.isEmpty() ? null : franjas.get(0).substring(0, 5),
                franjas.isEmpty() ? null : franjas.get(franjas.size() - 1).substring(6),
                franjas,
                horario.anticipacionMinima().toHours(),
                horario.horizonteDias(),
                horario.zona().getId());
    }
}
