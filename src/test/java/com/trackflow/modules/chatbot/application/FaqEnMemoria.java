package com.trackflow.modules.chatbot.application;

import com.trackflow.modules.chatbot.domain.PreguntaFrecuente;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Repositorio de prueba: asigna los identificadores como lo haría la base. */
class FaqEnMemoria implements FaqRepository {

    final List<PreguntaFrecuente> faqs = new ArrayList<>();
    private long siguiente = 1;

    @Override
    public PreguntaFrecuente save(PreguntaFrecuente faq) {
        if (faq.id() == null) {
            asignarId(faq, siguiente++);
            faqs.add(faq);
        }
        return faq;
    }

    @Override
    public Optional<PreguntaFrecuente> findById(Long id) {
        return faqs.stream().filter(f -> f.id().equals(id)).findFirst();
    }

    @Override
    public List<PreguntaFrecuente> todas() {
        return List.copyOf(faqs);
    }

    @Override
    public List<PreguntaFrecuente> activas() {
        return faqs.stream().filter(PreguntaFrecuente::activo).toList();
    }

    @Override
    public boolean existeIntencion(String intencion) {
        return faqs.stream().anyMatch(f -> f.intencion().equals(intencion));
    }

    private static void asignarId(PreguntaFrecuente faq, long id) {
        try {
            Field campo = PreguntaFrecuente.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(faq, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
