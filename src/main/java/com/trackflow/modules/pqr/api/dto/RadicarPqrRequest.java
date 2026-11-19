package com.trackflow.modules.pqr.api.dto;

import com.trackflow.modules.pqr.domain.TipoPqr;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RadicarPqrRequest(
        @NotNull(message = "el tipo es obligatorio (PETICION, QUEJA o RECLAMO)") TipoPqr tipo,
        @NotBlank(message = "la descripción es obligatoria")
        @Size(min = 20, max = 2000, message = "la descripción debe tener entre 20 y 2000 caracteres")
        String descripcion,
        @NotBlank(message = "el nombre es obligatorio")
        @Size(min = 2, max = 120, message = "el nombre debe tener entre 2 y 120 caracteres")
        String contactoNombre,
        @NotBlank(message = "el correo es obligatorio") @Email(message = "el correo no tiene un formato válido")
        @Size(max = 120, message = "el correo es demasiado largo")
        String contactoEmail,
        @Pattern(regexp = "^[0-9+ ]{7,20}$", message = "el teléfono debe tener entre 7 y 20 dígitos")
        String contactoTelefono,
        @Pattern(regexp = "^TF[0-9A-Z]{12}$", message = "el número de seguimiento no tiene el formato de una guía")
        String numeroSeguimiento,
        @AssertTrue(message = "debe aceptar el tratamiento de datos personales")
        boolean aceptaTratamientoDatos) {

    public RadicarPqrRequest {
        if (contactoTelefono != null && contactoTelefono.isBlank()) {
            contactoTelefono = null;
        }
        if (numeroSeguimiento != null && numeroSeguimiento.isBlank()) {
            numeroSeguimiento = null;
        }
    }
}
