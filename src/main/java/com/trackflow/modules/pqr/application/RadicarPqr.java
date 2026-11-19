package com.trackflow.modules.pqr.application;

import com.trackflow.modules.pqr.domain.Pqr;
import com.trackflow.modules.pqr.domain.TipoPqr;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HU-19: el remitente o el destinatario radica una PQR. */
@Service
public class RadicarPqr {

    private final PqrRepository pqrs;
    private final GeneradorDeRadicado radicados;
    private final PublicadorDePqr publicador;
    private final Clock clock;

    public RadicarPqr(PqrRepository pqrs, GeneradorDeRadicado radicados, PublicadorDePqr publicador, Clock clock) {
        this.pqrs = pqrs;
        this.radicados = radicados;
        this.publicador = publicador;
        this.clock = clock;
    }

    public record Solicitud(TipoPqr tipo, String descripcion, String nombre, String email, String telefono,
            String numeroSeguimiento, boolean aceptaTratamientoDatos) {
    }

    @Transactional
    public Pqr radicar(Solicitud solicitud) {
        Instant ahora = clock.instant();
        Pqr pqr = Pqr.radicar(radicados.siguiente(), solicitud.tipo(), TextoPlano.limpiar(solicitud.descripcion()),
                new Pqr.Contacto(TextoPlano.limpiar(solicitud.nombre()), solicitud.email(),
                        vacioANulo(solicitud.telefono())),
                vacioANulo(solicitud.numeroSeguimiento()), solicitud.aceptaTratamientoDatos(), ahora);
        pqrs.save(pqr);
        pqrs.saveEvento(pqr.eventoDeRadicacion());
        publicador.radicada(new PqrRadicada(pqr.radicado(), pqr.tipo(), pqr.contactoNombre(), pqr.contactoEmail(),
                ahora));
        return pqr;
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }
}
