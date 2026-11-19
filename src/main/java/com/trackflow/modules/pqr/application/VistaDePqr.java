package com.trackflow.modules.pqr.application;

import com.trackflow.modules.pqr.domain.Pqr;
import com.trackflow.modules.pqr.domain.PqrEvento;
import java.util.List;

/** Una PQR con su historial, tal como la necesitan las pantallas. */
public record VistaDePqr(Pqr pqr, List<PqrEvento> historial) {
}
