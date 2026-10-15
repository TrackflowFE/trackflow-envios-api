package com.trackflow.bootstrap;

import com.trackflow.modules.shipments.application.RepublicarEnviosCreados;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vuelve a publicar el alta de los envíos de esta API, para que quien mantenga
 * proyecciones a partir de esos eventos (la API de reportes) pueda reconstruirlas.
 *
 * Cada API que publica eventos tiene su propia operación: la de los eventos
 * logísticos vive en la API de logística, y el orden entre ambas importa, primero
 * los envíos y después sus movimientos.
 */
@RestController
@RequestMapping("/api/admin")
public class ReconstruccionController {

    private final RepublicarEnviosCreados envios;

    public ReconstruccionController(RepublicarEnviosCreados envios) {
        this.envios = envios;
    }

    @PostMapping("/rebuild-projections")
    public Map<String, Integer> reconstruir() {
        return Map.of("shipmentsRepublished", envios.ejecutar());
    }
}
