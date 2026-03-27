package com.acme.reservation.adapters.outbound.persistence;

import com.acme.reservation.domain.reservation.Reservation;
import com.acme.reservation.domain.reservation.ReservationStatus;
import com.acme.reservation.domain.reservation.RoomReservationItem;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JPA entity for the reservations table.
 */
@Entity
@Table(name = "reservations")
public class ReservationJpaEntity {

    @Id
    @Column(name = "reservation_id")
    private String reservationId;

    @Column(name = "partner_id", nullable = false)
    private String partnerId;

    @Column(name = "external_reference")
    private String externalReference;

    @Column(nullable = false)
    private String status;

    @Column(name = "currency_code", nullable = false)
    private String currencyCode;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Column(name = "total_refund_amount", nullable = false)
    private BigDecimal totalRefundAmount = BigDecimal.ZERO;

    @Column(name = "total_cancellation_fee", nullable = false)
    private BigDecimal totalCancellationFee = BigDecimal.ZERO;

    @Column(name = "room_count", nullable = false)
    private int roomCount;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    @Column(name = "updated_at", nullable = false)
    private String updatedAt;

    @Column(name = "guest_id", nullable = false)
    private String guestId;

    // Default constructor for JPA
    protected ReservationJpaEntity() {}

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.EAGER)
    private List<RoomItemJpaEntity> rooms = new ArrayList<>();

    public static ReservationJpaEntity from(Reservation domain) {
        ReservationJpaEntity entity = new ReservationJpaEntity();
        entity.reservationId = domain.getReservationId();
        entity.partnerId = domain.getPartnerId();
        entity.externalReference = domain.getExternalReference();
        entity.status = domain.getStatus().name();
        entity.currencyCode = domain.getCurrencyCode();
        entity.totalPrice = domain.getTotalPrice();
        entity.totalRefundAmount = domain.getTotalRefundAmount();
        entity.totalCancellationFee = domain.getTotalCancellationFee();
        entity.roomCount = domain.getRoomCount();
        entity.version = domain.getVersion();
        entity.createdAt = domain.getCreatedAt().toString();
        entity.updatedAt = domain.getUpdatedAt().toString();
        entity.guestId = domain.getGuestId();
        entity.rooms = domain.getRooms().stream()
                .map(r -> RoomItemJpaEntity.from(r, entity))
                .collect(Collectors.toList());
        return entity;
    }

    public Reservation toDomain() {
        return toDomain(rooms);
    }

    public Reservation toDomain(List<RoomItemJpaEntity> roomList) {
        List<RoomReservationItem> domainRooms = rooms.stream()
                .map(RoomItemJpaEntity::toDomain)
                .collect(Collectors.toList());
        return Reservation.builder()
                .reservationId(reservationId)
                .partnerId(partnerId)
                .externalReference(externalReference)
                .status(ReservationStatus.valueOf(status))
                .currencyCode(currencyCode)
                .totalPrice(totalPrice)
                .totalRefundAmount(totalRefundAmount)
                .totalCancellationFee(totalCancellationFee)
                .version(version)
                .createdAt(Instant.parse(createdAt))
                .updatedAt(Instant.parse(updatedAt))
                .guestId(guestId)
                .rooms(domainRooms)
                .build();
    }

    // Getters for test access
    public String getReservationId() {
        return reservationId;
    }

    public String getStatus() {
        return status;
    }
}
