package com.trackflow.bootstrap.security;

import com.trackflow.modules.shipments.application.RegistroDeAccesos;
import org.springframework.stereotype.Component;

/**
 * Lleva los accesos a datos personales completos al mismo registro de auditoría
 * que los cambios de usuarios y roles (HU-13), para que todo lo que hay que poder
 * revisar después esté en un solo sitio.
 */
@Component
public class RegistroDeAccesosAuditado implements RegistroDeAccesos {

    private final Auditorias auditorias;

    public RegistroDeAccesosAuditado(Auditorias auditorias) {
        this.auditorias = auditorias;
    }

    @Override
    public void accesoADatosCompletos(String actor, String trackingNumber) {
        auditorias.registrar(actor, Auditorias.DATOS_COMPLETOS_CONSULTADOS, trackingNumber,
                "consulta de datos personales completos del envío");
    }
}
