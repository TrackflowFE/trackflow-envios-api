package com.trackflow.modules.pickups.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Un cambio en la vida de una recogida: la bitácora.
 *
 * Guarda el estado anterior y el nuevo, quién lo hizo y cuándo. Con eso se responde
 * por qué una recogida está en el estado en que está, y no solo cómo está.
 */
@Entity
@Table(name = "pickup_events")
public class PickupEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pickup_number", nullable = false, length = 255)
    private String pickupNumber;

    @Column(name = "event_id", nullable = false, unique = true, length = 255)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 255)
    private PickupEventType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 255)
    private PickupStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 255)
    private PickupStatus newStatus;

    @Column(name = "detail", length = 500)
    private String detail;

    @Column(name = "actor_username", nullable = false, length = 50)
    private String actorUsername;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected PickupEvent() {
    }

    public PickupEvent(String eventId, String pickupNumber, PickupEventType type, PickupStatus previousStatus,
            PickupStatus newStatus, String detail, String actorUsername, Instant occurredAt) {
        this.eventId = eventId;
        this.pickupNumber = pickupNumber;
        this.type = type;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.detail = detail;
        this.actorUsername = actorUsername;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getPickupNumber() {
        return pickupNumber;
    }

    public PickupEventType getType() {
        return type;
    }

    public PickupStatus getPreviousStatus() {
        return previousStatus;
    }

    public PickupStatus getNewStatus() {
        return newStatus;
    }

    public String getDetail() {
        return detail;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
