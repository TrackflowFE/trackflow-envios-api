package com.trackflow.modules.shipments.application;

/**
 * Deja constancia de quién consultó datos personales completos (HU-13).
 *
 * Es un puerto: el módulo de envíos declara que necesita registrar el acceso, y
 * quien sabe cómo auditarlo lo implementa fuera, sin que este módulo dependa del
 * mecanismo de auditoría.
 */
public interface RegistroDeAccesos {

    void accesoADatosCompletos(String actor, String trackingNumber);
}
