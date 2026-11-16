package com.trackflow.modules.pickups.domain;

/**
 * Tamaño aproximado del paquete que se va a recoger.
 *
 * Es aproximado a propósito: sirve para dimensionar el vehículo y el tiempo de la
 * visita, no para facturar. Por eso es un catálogo cerrado y no un número.
 */
public enum PackageSize {
    SMALL,
    MEDIUM,
    LARGE
}
