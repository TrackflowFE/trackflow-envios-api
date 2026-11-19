package com.trackflow.modules.chatbot.application;

import com.trackflow.bootstrap.security.Auditorias;
import com.trackflow.modules.chatbot.domain.FaqNoEncontradaException;
import com.trackflow.modules.chatbot.domain.IntencionDuplicadaException;
import com.trackflow.modules.chatbot.domain.PreguntaFrecuente;
import com.trackflow.modules.chatbot.domain.PreguntaFrecuente.Contenido;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * HU-21: el administrador cambia el contenido del chatbot sin desplegar código.
 *
 * Cada cambio recarga el catálogo, pero solo después de confirmarse en la base: si la
 * transacción falla, el chatbot sigue con el contenido anterior. Cada cambio queda en
 * la auditoría con quién lo hizo; el texto de la respuesta no se copia ahí.
 */
@Service
public class AdministrarFaq {

    private final FaqRepository faqs;
    private final CatalogoDeFaq catalogo;
    private final Auditorias auditorias;
    private final Clock clock;

    public AdministrarFaq(FaqRepository faqs, CatalogoDeFaq catalogo, Auditorias auditorias, Clock clock) {
        this.faqs = faqs;
        this.catalogo = catalogo;
        this.auditorias = auditorias;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PreguntaFrecuente> listar() {
        return faqs.todas();
    }

    @Transactional
    public PreguntaFrecuente crear(String intencion, Contenido contenido, String actor) {
        if (faqs.existeIntencion(intencion)) {
            throw new IntencionDuplicadaException(intencion);
        }
        PreguntaFrecuente faq = faqs.save(PreguntaFrecuente.crear(intencion, contenido, actor, clock.instant()));
        terminar(Auditorias.FAQ_CREADA, actor, faq);
        return faq;
    }

    @Transactional
    public PreguntaFrecuente editar(Long id, Contenido contenido, String actor) {
        PreguntaFrecuente faq = buscar(id);
        faq.editar(contenido, actor, clock.instant());
        terminar(Auditorias.FAQ_EDITADA, actor, faqs.save(faq));
        return faq;
    }

    @Transactional
    public PreguntaFrecuente cambiarActivo(Long id, boolean activo, String actor) {
        PreguntaFrecuente faq = buscar(id);
        faq.cambiarActivo(activo, actor, clock.instant());
        terminar(activo ? Auditorias.FAQ_ACTIVADA : Auditorias.FAQ_DESACTIVADA, actor, faqs.save(faq));
        return faq;
    }

    private PreguntaFrecuente buscar(Long id) {
        return faqs.findById(id).orElseThrow(() -> new FaqNoEncontradaException(id));
    }

    private void terminar(String accion, String actor, PreguntaFrecuente faq) {
        auditorias.registrar(actor, accion, faq.intencion(), "categoria=" + faq.categoria());
        recargarAlConfirmar();
    }

    private void recargarAlConfirmar() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            catalogo.recargar();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                catalogo.recargar();
            }
        });
    }
}
