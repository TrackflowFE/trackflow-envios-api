package com.trackflow.modules.shipments.domain;

/**
 * Las etapas del recorrido, en orden.
 *
 * El centro de destino tiene estado propio aunque físicamente también sea "estar en
 * un centro": mientras los dos compartieron AT_DISTRIBUTION_CENTER, llegar a la ciudad
 * de destino parecía un retroceso al paso anterior para cualquiera que dibujara el
 * progreso a partir del estado. Con un valor distinto para cada etapa, avanzar es lo
 * único que puede pasar.
 *
 * IN_CUSTODY es la etapa de los envíos que se recogieron en la dirección del
 * remitente (HU-20): el paquete ya está en la empresa, pero todavía no ha entrado a
 * un centro. Por eso va justo después de REGISTERED y antes que el centro de origen.
 */
public enum ShipmentStatus {
    REGISTERED,
    IN_CUSTODY,
    AT_DISTRIBUTION_CENTER,
    IN_TRANSIT,
    AT_DESTINATION_CENTER,
    OUT_FOR_DELIVERY,
    DELIVERED
}
