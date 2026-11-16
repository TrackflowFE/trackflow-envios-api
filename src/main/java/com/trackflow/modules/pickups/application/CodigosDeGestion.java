package com.trackflow.modules.pickups.application;

public interface CodigosDeGestion {
    String generar();
    String hash(String codigo);
    boolean coincide(String codigo, String hash);
}
