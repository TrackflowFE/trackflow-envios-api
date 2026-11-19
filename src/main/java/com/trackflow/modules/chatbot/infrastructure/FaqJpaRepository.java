package com.trackflow.modules.chatbot.infrastructure;

import com.trackflow.modules.chatbot.domain.PreguntaFrecuente;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaqJpaRepository extends JpaRepository<PreguntaFrecuente, Long> {

    List<PreguntaFrecuente> findAllByOrderByOrdenAscIdAsc();

    List<PreguntaFrecuente> findByActivoTrueOrderByOrdenAscIdAsc();

    boolean existsByIntencion(String intencion);
}
