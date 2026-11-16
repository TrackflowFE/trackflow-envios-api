package com.trackflow.modules.pickups.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Número de una recogida a domicilio: RC seguido de 12 caracteres alfanuméricos.
 *
 * Lleva prefijo propio para que no se confunda con una guía de envío (TF) ni se
 * acepten por error: quien teclea el número de un envío donde se espera el de una
 * recogida recibe un error de formato, no una búsqueda sin resultados.
 */
@Embeddable
public class PickupNumber {

    public static final String PREFIX = "RC";
    private static final Pattern FORMAT = Pattern.compile("^RC[0-9A-Z]{12}$");

    @Column(name = "pickup_number", nullable = false, unique = true, updatable = false)
    private String value;

    protected PickupNumber() {
    }

    private PickupNumber(String value) {
        this.value = value;
    }

    public static PickupNumber of(String value) {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Número de recogida inválido: " + value);
        }
        return new PickupNumber(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PickupNumber that && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
