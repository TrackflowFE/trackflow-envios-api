package com.trackflow.modules.pqr.api;

import com.trackflow.modules.pqr.api.dto.CambiarEstadoRequest;
import com.trackflow.modules.pqr.api.dto.PqrDetalleResponse;
import com.trackflow.modules.pqr.api.dto.PqrResumenResponse;
import com.trackflow.modules.pqr.application.ConsultarPqr;
import com.trackflow.modules.pqr.application.GestionarPqr;
import com.trackflow.modules.pqr.domain.EstadoPqr;
import com.trackflow.modules.pqr.domain.TipoPqr;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Lo que hace quien atiende las PQR (supervisor o administrador). */
@RestController
@RequestMapping("/api/pqr")
public class PqrGestionController {

    private static final int TAMANO_MAXIMO = 50;
    private static final String SIN_TOKEN = "sistema-local";

    private final ConsultarPqr consultar;
    private final GestionarPqr gestionar;

    public PqrGestionController(ConsultarPqr consultar, GestionarPqr gestionar) {
        this.consultar = consultar;
        this.gestionar = gestionar;
    }

    @GetMapping
    public List<PqrResumenResponse> bandeja(@RequestParam(required = false) EstadoPqr estado,
            @RequestParam(required = false) TipoPqr tipo, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int pagina = Math.max(page, 0);
        int tamano = Math.min(Math.max(size, 1), TAMANO_MAXIMO);
        return consultar.listar(estado, tipo, pagina, tamano).stream().map(PqrResumenResponse::from).toList();
    }

    @GetMapping("/{radicado}")
    public PqrDetalleResponse detalle(@PathVariable String radicado) {
        return PqrDetalleResponse.from(consultar.detalle(radicado));
    }

    /** El actor es el usuario del token; sin protección activa (entorno local) no hay token. */
    @PostMapping("/{radicado}/estado")
    public PqrDetalleResponse cambiarEstado(@PathVariable String radicado,
            @Valid @RequestBody CambiarEstadoRequest request, Principal principal) {
        String actor = principal == null ? SIN_TOKEN : principal.getName();
        return PqrDetalleResponse.from(gestionar.cambiarEstado(radicado, request.estado(), request.respuesta(),
                actor));
    }
}
