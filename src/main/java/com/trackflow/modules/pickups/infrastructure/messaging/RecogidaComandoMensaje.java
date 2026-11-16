package com.trackflow.modules.pickups.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.trackflow.modules.pickups.application.RecogidaComando;
import com.trackflow.modules.pickups.domain.PackageSize;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Contrato del mensaje que viaja por RabbitMQ, independiente del modelo de dominio.
 *
 * Es un lector tolerante: ignora campos que no conoce, en vez de mandarlos a la cola
 * de descartados. Cada comando usa el subconjunto de campos que le corresponde y los
 * demás vienen en null.
 */
@JsonIgnoreProperties(value = "version", allowGetters = true, ignoreUnknown = true)
public record RecogidaComandoMensaje(
        String eventId,
        String comando,
        String pickupNumber,
        String actor,
        Instant occurredAt,
        String address,
        Long cityId,
        String contactName,
        String contactPhone,
        LocalDate scheduledDate,
        String timeSlot,
        String packageSize,
        BigDecimal weightKg,
        String shipmentTrackingNumber,
        String assignedUsername,
        String reason,
        String managementCodeHash) {

    /**
     * Versión del formato con que se produjo el mensaje. Viaja en cada mensaje para
     * que un consumidor futuro pueda distinguir formatos; al leer se ignora.
     */
    public static final int VERSION = 1;

    @JsonProperty("version")
    public int schemaVersion() {
        return VERSION;
    }

    public static RecogidaComandoMensaje from(RecogidaComando comando) {
        return new RecogidaComandoMensaje(
                comando.eventId(),
                comando.comando().name(),
                comando.pickupNumber(),
                comando.actor(),
                comando.occurredAt(),
                comando.address(),
                comando.cityId(),
                comando.contactName(),
                comando.contactPhone(),
                comando.scheduledDate(),
                comando.timeSlot(),
                comando.packageSize() == null ? null : comando.packageSize().name(),
                comando.weightKg(),
                comando.shipmentTrackingNumber(),
                comando.assignedUsername(),
                comando.reason(),
                comando.managementCodeHash());
    }

    public RecogidaComando toComando() {
        return new RecogidaComando(
                eventId,
                RecogidaComando.Tipo.valueOf(comando),
                pickupNumber,
                actor,
                occurredAt,
                address,
                cityId,
                contactName,
                contactPhone,
                scheduledDate,
                timeSlot,
                packageSize == null ? null : PackageSize.valueOf(packageSize),
                weightKg,
                shipmentTrackingNumber,
                assignedUsername,
                reason,
                managementCodeHash);
    }
}
