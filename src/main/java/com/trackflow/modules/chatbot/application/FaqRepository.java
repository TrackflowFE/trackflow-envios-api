package com.trackflow.modules.chatbot.application;

import com.trackflow.modules.chatbot.domain.PreguntaFrecuente;
import java.util.List;
import java.util.Optional;

public interface FaqRepository {

    PreguntaFrecuente save(PreguntaFrecuente faq);

    Optional<PreguntaFrecuente> findById(Long id);

    /** Todas, activas o no, para el editor del administrador. */
    List<PreguntaFrecuente> todas();

    /** Solo las que el chatbot puede usar. */
    List<PreguntaFrecuente> activas();

    boolean existeIntencion(String intencion);
}
