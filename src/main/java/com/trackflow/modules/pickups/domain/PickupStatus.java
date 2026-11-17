package com.trackflow.modules.pickups.domain;

/**
 * Las etapas de una recogida a domicilio, en orden.
 *
 * Son las mismas que ve el operador, con otro nombre: una recogida se solicita, se
 * programa, el operador va en camino, se recoge y ya no hay nada que hacer. Recogida y
 * cancelada son finales.
 */
public enum PickupStatus {
    REQUESTED,
    SCHEDULED,
    ON_THE_WAY,
    PICKED_UP,
    CANCELLED;

    /** En estos estados no se puede volver a tocar la recogida. */
    public boolean esTerminal() {
        return this == PICKED_UP || this == CANCELLED;
    }

    /** Se puede cambiar la fecha o la franja mientras nadie haya salido a recoger. */
    public boolean admiteReprogramacion() {
        return this == REQUESTED || this == SCHEDULED;
    }

    public boolean admiteCancelacion() {
        return this == REQUESTED || this == SCHEDULED;
    }

    /** Solo sale a recoger una recogida que ya tiene fecha y responsable. */
    public boolean admiteDespacho() {
        return this == SCHEDULED;
    }

    /** La recogida se marca como hecha cuando el operador ya iba en camino. */
    public boolean admiteCompletado() {
        return this == ON_THE_WAY;
    }

    /** Un envío se puede vincular a una recogida mientras esta no esté cancelada. */
    public boolean admiteVinculacion() {
        return this != CANCELLED;
    }
}
