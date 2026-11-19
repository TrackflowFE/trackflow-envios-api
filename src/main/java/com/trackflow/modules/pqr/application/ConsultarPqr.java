package com.trackflow.modules.pqr.application;

import com.trackflow.modules.pqr.domain.EstadoPqr;
import com.trackflow.modules.pqr.domain.Pqr;
import com.trackflow.modules.pqr.domain.PqrNoEncontradaException;
import com.trackflow.modules.pqr.domain.TipoPqr;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HU-19: consultar el estado de una PQR, como solicitante o como quien la atiende. */
@Service
@Transactional(readOnly = true)
public class ConsultarPqr {

    private final PqrRepository pqrs;

    public ConsultarPqr(PqrRepository pqrs) {
        this.pqrs = pqrs;
    }

    /**
     * El solicitante necesita el radicado y el correo con que radicó. Si falla
     * cualquiera de los dos el resultado es el mismo, para no revelar qué radicados
     * existen.
     */
    public VistaDePqr delSolicitante(String radicado, String email) {
        Pqr pqr = pqrs.findByRadicado(radicado == null ? "" : radicado.strip().toUpperCase())
                .filter(p -> p.esDe(email))
                .orElseThrow(PqrNoEncontradaException::new);
        return new VistaDePqr(pqr, pqrs.historial(pqr.radicado()));
    }

    public VistaDePqr detalle(String radicado) {
        Pqr pqr = pqrs.findByRadicado(radicado).orElseThrow(PqrNoEncontradaException::new);
        return new VistaDePqr(pqr, pqrs.historial(radicado));
    }

    public List<Pqr> listar(EstadoPqr estado, TipoPqr tipo, int pagina, int tamano) {
        return pqrs.search(estado, tipo, pagina, tamano);
    }
}
