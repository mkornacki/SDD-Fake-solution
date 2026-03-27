package com.acme.reservation.application.ports.inbound;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Inbound port: create a new multi-room reservation.
 */
public interface CreateReservationUseCase {

        final class RoomRequest {
                private final String roomCode;
                private final LocalDate checkInDate;
                private final LocalDate checkOutDate;
                private final BigDecimal basePrice;

                public RoomRequest(
                                String roomCode,
                                LocalDate checkInDate,
                                LocalDate checkOutDate,
                                BigDecimal basePrice) {
                        this.roomCode = roomCode;
                        this.checkInDate = checkInDate;
                        this.checkOutDate = checkOutDate;
                        this.basePrice = basePrice;
                }

                public String roomCode() {
                        return roomCode;
                }

                public LocalDate checkInDate() {
                        return checkInDate;
                }

                public LocalDate checkOutDate() {
                        return checkOutDate;
                }

                public BigDecimal basePrice() {
                        return basePrice;
                }
        }

        final class GuestInfo {
                private final String givenName;
                private final String familyName;
                private final String email;
                private final String phone;

                public GuestInfo(String givenName, String familyName, String email, String phone) {
                        this.givenName = givenName;
                        this.familyName = familyName;
                        this.email = email;
                        this.phone = phone;
                }

                public String givenName() {
                        return givenName;
                }

                public String familyName() {
                        return familyName;
                }

                public String email() {
                        return email;
                }

                public String phone() {
                        return phone;
                }
        }

        final class Command {
                private final String idempotencyKey;
                private final String partnerId;
                private final String externalReference;
                private final String currencyCode;
                private final GuestInfo guest;
                private final List<RoomRequest> rooms;
                private final String actorId;
                private final String traceId;

                public Command(
                                String idempotencyKey,
                                String partnerId,
                                String externalReference,
                                String currencyCode,
                                GuestInfo guest,
                                List<RoomRequest> rooms,
                                String actorId,
                                String traceId) {
                        this.idempotencyKey = idempotencyKey;
                        this.partnerId = partnerId;
                        this.externalReference = externalReference;
                        this.currencyCode = currencyCode;
                        this.guest = guest;
                        this.rooms = rooms;
                        this.actorId = actorId;
                        this.traceId = traceId;
                }

                public String idempotencyKey() {
                        return idempotencyKey;
                }

                public String partnerId() {
                        return partnerId;
                }

                public String externalReference() {
                        return externalReference;
                }

                public String currencyCode() {
                        return currencyCode;
                }

                public GuestInfo guest() {
                        return guest;
                }

                public List<RoomRequest> rooms() {
                        return rooms;
                }

                public String actorId() {
                        return actorId;
                }

                public String traceId() {
                        return traceId;
                }
        }

        final class Result {
                private final String reservationId;
                private final String status;
                private final String partnerProcessingStatus;
                private final boolean idempotentReplay;

                public Result(
                                String reservationId,
                                String status,
                                String partnerProcessingStatus,
                                boolean idempotentReplay) {
                        this.reservationId = reservationId;
                        this.status = status;
                        this.partnerProcessingStatus = partnerProcessingStatus;
                        this.idempotentReplay = idempotentReplay;
                }

                public String reservationId() {
                        return reservationId;
                }

                public String status() {
                        return status;
                }

                public String partnerProcessingStatus() {
                        return partnerProcessingStatus;
                }

                public boolean idempotentReplay() {
                        return idempotentReplay;
                }
        }

    Result execute(Command command);
}
