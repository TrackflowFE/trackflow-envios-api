package com.trackflow.modules.chatbot.infrastructure;

import com.trackflow.modules.chatbot.application.FaqRepository;
import com.trackflow.modules.chatbot.domain.PreguntaFrecuente;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaFaqRepository implements FaqRepository {

    private final FaqJpaRepository jpa;

    public JpaFaqRepository(FaqJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public PreguntaFrecuente save(PreguntaFrecuente faq) {
        return jpa.save(faq);
    }

    @Override
    public Optional<PreguntaFrecuente> findById(Long id) {
        return jpa.findById(id);
    }

    @Override
    public List<PreguntaFrecuente> todas() {
        return jpa.findAllByOrderByOrdenAscIdAsc();
    }

    @Override
    public List<PreguntaFrecuente> activas() {
        return jpa.findByActivoTrueOrderByOrdenAscIdAsc();
    }

    @Override
    public boolean existeIntencion(String intencion) {
        return jpa.existsByIntencion(intencion);
    }
}
