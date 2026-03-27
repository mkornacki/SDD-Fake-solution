package com.acme.reservation.domain.reservation;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable audit record for reservation and room-level state transitions.
 * Append-only — never mutated after creation.
 * Domain entity — no Spring or ORM dependencies.
 */
public final class ReservationHistoryEvent {

    public enum EventType {
        CREATED,
        ROOM_CANCELLED,
        RESERVATION_CANCELLED,
        STATUS_CHANGED,
        FINANCIAL_RECALCULATED,
        ASYNC_FAILED,
        REPLAYED
    }

    public enum ActorType {
        USER, PARTNER, SERVICE, SYSTEM
    }

    private final String eventId;
    private final String reservationId;
    private final String roomItemId;
    private final EventType eventType;
    private final String actorId;
    private final ActorType actorType;
    private final String reason;
    private final String beforeStateRef;
    private final String afterStateRef;
    private final String traceId;
    private final Instant occurredAt;

    private ReservationHistoryEvent(Builder builder) {
        this.eventId = Objects.requireNonNull(builder.eventId, "eventId required");
        this.reservationId = Objects.requireNonNull(builder.reservationId, "reservationId required");
        this.roomItemId = builder.roomItemId;
        this.eventType = Objects.requireNonNull(builder.eventType, "eventType required");
        this.actorId = Objects.requireNonNull(builder.actorId, "actorId required");
        this.actorType = Objects.requireNonNull(builder.actorType, "actorType required");
        this.reason = builder.reason;
        this.beforeStateRef = builder.beforeStateRef;
        this.afterStateRef = builder.afterStateRef;
        this.traceId = Objects.requireNonNull(builder.traceId, "traceId required");
        this.occurredAt = Objects.requireNonNull(builder.occurredAt, "occurredAt required");
    }

    // No setters — append-only entity

    public String getEventId() {
        return eventId;
    }

    public String getReservationId() {
        return reservationId;
    }

    public String getRoomItemId() {
        return roomItemId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getActorId() {
        return actorId;
    }

    public ActorType getActorType() {
        return actorType;
    }

    public String getReason() {
        return reason;
    }

    public String getBeforeStateRef() {
        return beforeStateRef;
    }

    public String getAfterStateRef() {
        return afterStateRef;
    }

    public String getTraceId() {
        return traceId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String eventId;
        private String reservationId;
        private String roomItemId;
        private EventType eventType;
        private String actorId;
        private ActorType actorType;
        private String reason;
        private String beforeStateRef;
        private String afterStateRef;
        private String traceId;
        private Instant occurredAt;

        public Builder eventId(String v) {
            this.eventId = v;
            return this;
        }

        public Builder reservationId(String v) {
            this.reservationId = v;
            return this;
        }

        public Builder roomItemId(String v) {
            this.roomItemId = v;
            return this;
        }

        public Builder eventType(EventType v) {
            this.eventType = v;
            return this;
        }

        public Builder actorId(String v) {
            this.actorId = v;
            return this;
        }

        public Builder actorType(ActorType v) {
            this.actorType = v;
            return this;
        }

        public Builder reason(String v) {
            this.reason = v;
            return this;
        }

        public Builder beforeStateRef(String v) {
            this.beforeStateRef = v;
            return this;
        }

        public Builder afterStateRef(String v) {
            this.afterStateRef = v;
            return this;
        }

        public Builder traceId(String v) {
            this.traceId = v;
            return this;
        }

        public Builder occurredAt(Instant v) {
            this.occurredAt = v;
            return this;
        }

        public ReservationHistoryEvent build() {
            if (eventId == null) {
                eventId = UUID.randomUUID().toString();
            }
            if (occurredAt == null) {
                occurredAt = Instant.now();
            }
            return new ReservationHistoryEvent(this);
        }
    }
}
