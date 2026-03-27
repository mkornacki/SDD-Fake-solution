package com.acme.reservation.adapters.inbound.http.reservation;

import com.acme.reservation.application.ports.inbound.CancelReservationUseCase;
import com.acme.reservation.application.ports.inbound.CancelRoomUseCase;
import com.acme.reservation.application.ports.inbound.CreateReservationUseCase;
import com.acme.reservation.application.ports.inbound.GetReservationUseCase;
import com.acme.reservation.application.ports.outbound.RequestContextRepository;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * HTTP inbound adapter for the Reservation API.
 * Implements all four reservation lifecycle endpoints as per contracts/reservation-api.yaml.
 */
@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final CreateReservationUseCase createReservationUseCase;
    private final GetReservationUseCase getReservationUseCase;
    private final CancelRoomUseCase cancelRoomUseCase;
    private final CancelReservationUseCase cancelReservationUseCase;
    private final ReservationResponseMapper responseMapper;
    private final RequestContextRepository requestContextRepository;
    private final Bulkhead reservationCreateBulkhead;

    public ReservationController(
            CreateReservationUseCase createReservationUseCase,
            GetReservationUseCase getReservationUseCase,
            CancelRoomUseCase cancelRoomUseCase,
            CancelReservationUseCase cancelReservationUseCase,
            ReservationResponseMapper responseMapper,
            RequestContextRepository requestContextRepository,
            BulkheadRegistry bulkheadRegistry) {
        this.createReservationUseCase = createReservationUseCase;
        this.getReservationUseCase = getReservationUseCase;
        this.cancelRoomUseCase = cancelRoomUseCase;
        this.cancelReservationUseCase = cancelReservationUseCase;
        this.responseMapper = responseMapper;
        this.requestContextRepository = requestContextRepository;
        this.reservationCreateBulkhead = bulkheadRegistry.bulkhead("reservationCreateHttp");
    }

    // --- Request/Response DTOs ---

    public static class GuestInfoDto {
        @NotBlank
        private String givenName;
        @NotBlank
        private String familyName;
        private String email;
        private String phone;

        public GuestInfoDto() {
        }

        public GuestInfoDto(String givenName, String familyName, String email, String phone) {
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

        public void setGivenName(String givenName) {
            this.givenName = givenName;
        }

        public void setFamilyName(String familyName) {
            this.familyName = familyName;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
    }

    public static class RoomRequestDto {
        @NotBlank
        private String roomCode;
        @NotNull
        private LocalDate checkInDate;
        @NotNull
        private LocalDate checkOutDate;
        @NotNull
        @Positive
        private BigDecimal basePrice;

        public RoomRequestDto() {
        }

        public RoomRequestDto(String roomCode, LocalDate checkInDate, LocalDate checkOutDate, BigDecimal basePrice) {
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

        public void setRoomCode(String roomCode) {
            this.roomCode = roomCode;
        }

        public void setCheckInDate(LocalDate checkInDate) {
            this.checkInDate = checkInDate;
        }

        public void setCheckOutDate(LocalDate checkOutDate) {
            this.checkOutDate = checkOutDate;
        }

        public void setBasePrice(BigDecimal basePrice) {
            this.basePrice = basePrice;
        }
    }

    public static class CreateReservationRequest {
        @NotBlank
        private String partnerId;
        private String externalReference;
        @NotBlank
        private String currencyCode;
        @NotNull
        @Valid
        private GuestInfoDto guest;
        @NotEmpty
        @Valid
        private List<RoomRequestDto> rooms;

        public CreateReservationRequest() {
        }

        public CreateReservationRequest(
                String partnerId,
                String externalReference,
                String currencyCode,
                GuestInfoDto guest,
                List<RoomRequestDto> rooms) {
            this.partnerId = partnerId;
            this.externalReference = externalReference;
            this.currencyCode = currencyCode;
            this.guest = guest;
            this.rooms = rooms;
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

        public GuestInfoDto guest() {
            return guest;
        }

        public List<RoomRequestDto> rooms() {
            return rooms;
        }

        public void setPartnerId(String partnerId) {
            this.partnerId = partnerId;
        }

        public void setExternalReference(String externalReference) {
            this.externalReference = externalReference;
        }

        public void setCurrencyCode(String currencyCode) {
            this.currencyCode = currencyCode;
        }

        public void setGuest(GuestInfoDto guest) {
            this.guest = guest;
        }

        public void setRooms(List<RoomRequestDto> rooms) {
            this.rooms = rooms;
        }
    }

    public static final class CreateReservationResponse {
        private final String reservationId;
        private final String status;
        private final String partnerProcessingStatus;

        public CreateReservationResponse(String reservationId, String status, String partnerProcessingStatus) {
            this.reservationId = reservationId;
            this.status = status;
            this.partnerProcessingStatus = partnerProcessingStatus;
        }

        public String reservationId() {
            return reservationId;
        }

        public String getReservationId() {
            return reservationId;
        }

        public String status() {
            return status;
        }

        public String getStatus() {
            return status;
        }

        public String partnerProcessingStatus() {
            return partnerProcessingStatus;
        }

        public String getPartnerProcessingStatus() {
            return partnerProcessingStatus;
        }
    }

    public static class CancellationRequest {
        private String reason;

        public CancellationRequest() {
        }

        public CancellationRequest(String reason) {
            this.reason = reason;
        }

        public String reason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static final class CancellationResponse {
        private final String reservationId;
        private final String status;

        public CancellationResponse(String reservationId, String status) {
            this.reservationId = reservationId;
            this.status = status;
        }

        public String reservationId() {
            return reservationId;
        }

        public String getReservationId() {
            return reservationId;
        }

        public String status() {
            return status;
        }

        public String getStatus() {
            return status;
        }
    }

    public static final class StatusResponse {
        private final String contextId;
        private final String status;
        private final String message;

        public StatusResponse(String contextId, String status, String message) {
            this.contextId = contextId;
            this.status = status;
            this.message = message;
        }

        public String contextId() {
            return contextId;
        }

        public String getContextId() {
            return contextId;
        }

        public String status() {
            return status;
        }

        public String getStatus() {
            return status;
        }

        public String message() {
            return message;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * POST /api/v1/reservations
     * Returns 202 for new reservations, 200 for idempotent replays.
     */
    @PostMapping
    public ResponseEntity<CreateReservationResponse> createReservation(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @Valid @RequestBody CreateReservationRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String actorId = jwt != null ? jwt.getSubject() : "anonymous";
        String traceId = correlationId != null ? correlationId : UUID.randomUUID().toString();

        List<CreateReservationUseCase.RoomRequest> roomRequests = request.rooms().stream()
                .map(r -> new CreateReservationUseCase.RoomRequest(
                        r.roomCode(), r.checkInDate(), r.checkOutDate(), r.basePrice()))
                .collect(Collectors.toList());

        CreateReservationUseCase.GuestInfo guestInfo = new CreateReservationUseCase.GuestInfo(
                request.guest().givenName(),
                request.guest().familyName(),
                request.guest().email(),
                request.guest().phone());

        CreateReservationUseCase.Command command = new CreateReservationUseCase.Command(
                idempotencyKey,
                request.partnerId(),
                request.externalReference(),
                request.currencyCode(),
                guestInfo,
                roomRequests,
                actorId,
                traceId);

        try {
            CreateReservationUseCase.Result result = Bulkhead
                    .decorateSupplier(
                            reservationCreateBulkhead,
                            () -> createReservationUseCase.execute(command))
                    .get();

            CreateReservationResponse response = new CreateReservationResponse(
                    result.reservationId(), result.status(), result.partnerProcessingStatus());

            if (result.idempotentReplay()) {
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.accepted().body(response);
        } catch (BulkheadFullException bulkheadFullException) {
            return ResponseEntity.status(503).build();
        }
    }

    /**
     * GET /api/v1/reservations/{reservationId}
     */
    @GetMapping("/{reservationId}")
    public ResponseEntity<Object> getReservation(
            @PathVariable String reservationId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @AuthenticationPrincipal Jwt jwt) {

        String actorId = jwt != null ? jwt.getSubject() : "anonymous";
        boolean hasPiiAccess = hasPiiScope(jwt);

        GetReservationUseCase.Query query = new GetReservationUseCase.Query(
                reservationId, actorId, hasPiiAccess);

        GetReservationUseCase.Result result = getReservationUseCase.execute(query);

        return ResponseEntity.ok(responseMapper.toResponse(result, hasPiiAccess));
    }

    /**
     * GET /api/v1/reservations/{contextId}/status
     * Poll the processing status of an async reservation request by context ID.
     * Returns 200 OK with status if context exists, 404 Not Found otherwise.
     */
    @GetMapping("/{contextId}/status")
    public ResponseEntity<StatusResponse> getReservationStatus(
            @PathVariable String contextId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        return requestContextRepository.findByContextId(contextId)
                .map(context -> {
                    String message;
                    switch (context.getStatus()) {
                        case RECEIVED:
                        case VALIDATED:
                        case QUEUED:
                            message = "Processing reservation...";
                            break;
                        case PROCESSING:
                            message = "Contacting partner hotel...";
                            break;
                        case COMPLETED:
                            message = "Reservation confirmed";
                            break;
                        case FAILED:
                            message = "Reservation failed: " + context.getErrorContext();
                            break;
                        case DLQ:
                            message = "Reservation queued for manual review: " + context.getErrorContext();
                            break;
                        default:
                            message = "Unknown status";
                    }
                    return ResponseEntity.ok(new StatusResponse(
                            contextId,
                            context.getStatus().name(),
                            message));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/v1/reservations/{reservationId}/rooms/{roomItemId}
     */
    @DeleteMapping("/{reservationId}/rooms/{roomItemId}")
    public ResponseEntity<CancellationResponse> cancelRoom(
            @PathVariable String reservationId,
            @PathVariable String roomItemId,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @RequestBody(required = false) CancellationRequest body,
            @AuthenticationPrincipal Jwt jwt) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String actorId = jwt != null ? jwt.getSubject() : "anonymous";
        String traceId = correlationId != null ? correlationId : UUID.randomUUID().toString();
        String reason = body != null ? body.reason() : null;

        CancelRoomUseCase.Command command = new CancelRoomUseCase.Command(
                reservationId, roomItemId, idempotencyKey, reason, actorId, traceId);

        CancelRoomUseCase.Result result = cancelRoomUseCase.execute(command);

        CancellationResponse response = new CancellationResponse(
                result.reservationId(), result.status());

        return result.idempotentReplay()
                ? ResponseEntity.ok(response)
                : ResponseEntity.accepted().body(response);
    }

    /**
     * DELETE /api/v1/reservations/{reservationId}
     */
    @DeleteMapping("/{reservationId}")
    public ResponseEntity<CancellationResponse> cancelReservation(
            @PathVariable String reservationId,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @RequestBody(required = false) CancellationRequest body,
            @AuthenticationPrincipal Jwt jwt) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String actorId = jwt != null ? jwt.getSubject() : "anonymous";
        String traceId = correlationId != null ? correlationId : UUID.randomUUID().toString();
        String reason = body != null ? body.reason() : null;

        CancelReservationUseCase.Command command = new CancelReservationUseCase.Command(
                reservationId, idempotencyKey, reason, actorId, traceId);

        CancelReservationUseCase.Result result = cancelReservationUseCase.execute(command);

        CancellationResponse response = new CancellationResponse(
                result.reservationId(), result.status());

        return result.idempotentReplay()
                ? ResponseEntity.ok(response)
                : ResponseEntity.accepted().body(response);
    }

    private boolean hasPiiScope(Jwt jwt) {
        if (jwt != null) {
            Object scopes = jwt.getClaims().get("scope");
            if (scopes instanceof String) {
                String scopeValue = (String) scopes;
                return scopeValue.contains("pii:read");
            }
            if (scopes instanceof List<?>) {
                List<?> list = (List<?>) scopes;
                return list.stream().anyMatch(s -> "pii:read".equals(s));
            }
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities() != null) {
            return authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(a -> "SCOPE_pii:read".equals(a) || "pii:read".equals(a));
        }
        return false;
    }
}
