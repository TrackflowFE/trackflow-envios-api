package com.trackflow.modules.pickups.api.dto;

import com.trackflow.modules.pickups.domain.PackageSize;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SolicitarRecogidaRequest(
        @NotBlank(message = "la dirección es obligatoria") @Size(min = 5, max = 200, message = "la dirección es demasiado corta o larga")
        String address,
        @NotNull(message = "la ciudad es obligatoria") Long cityId,
        @NotBlank(message = "el nombre de quien recibe es obligatorio") @Size(min = 3, max = 120)
        String contactName,
        @NotBlank(message = "el teléfono de contacto es obligatorio")
        @Pattern(regexp = "^[0-9]{7,20}$", message = "el teléfono debe tener entre 7 y 20 dígitos")
        String contactPhone,
        @NotNull(message = "la fecha es obligatoria")
        LocalDate scheduledDate,
        @NotBlank(message = "la franja horaria es obligatoria")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d-([01]\\d|2[0-3]):[0-5]\\d$",
                message = "la franja debe tener el formato HH:MM-HH:MM")
        String timeSlot,
        @NotNull(message = "el tamaño del paquete es obligatorio") PackageSize packageSize,
        @NotNull(message = "el peso aproximado es obligatorio")
        @DecimalMin(value = "0.1", message = "el peso debe ser mayor que cero")
        @DecimalMax(value = "100", message = "el peso máximo por paquete es de 100 kilos")
        BigDecimal weightKg,
        @Pattern(regexp = "^TF[0-9A-Z]{12}$", message = "el número de seguimiento no tiene el formato de una guía")
        String shipmentTrackingNumber) {

    public SolicitarRecogidaRequest {
        if (shipmentTrackingNumber != null && shipmentTrackingNumber.isBlank()) {
            shipmentTrackingNumber = null;
        }
    }
}
