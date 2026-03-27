package com.acme.reservation.adapters.inbound.http.reservation;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Runtime representation of caller's PII authorization scope derived from JWT.
 * Used to govern masking/unmasking in API responses.
 * Not persisted.
 */
public class PIIAccessContext {

    private final String callerId;
    private final List<String> allowedScopes;
    private final boolean hasPIIAccess;

    public PIIAccessContext(String callerId, List<String> allowedScopes) {
        this.callerId = callerId;
        this.allowedScopes = allowedScopes != null ? List.copyOf(allowedScopes) : List.of();
        this.hasPIIAccess = this.allowedScopes.contains("pii:read");
    }

    public static PIIAccessContext from(Jwt jwt) {
        if (jwt == null) {
            return new PIIAccessContext("anonymous", List.of());
        }
        String callerId = jwt.getSubject();
        List<String> scopes = extractScopes(jwt);
        return new PIIAccessContext(callerId, scopes);
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractScopes(Jwt jwt) {
        Object scopeClaim = jwt.getClaims().get("scope");
        if (scopeClaim instanceof String) {
            String value = (String) scopeClaim;
            return List.of(value.split(" "));
        }
        if (scopeClaim instanceof List<?>) {
            List<?> list = (List<?>) scopeClaim;
            return (List<String>) list;
        }
        return List.of();
    }

    public String getCallerId() {
        return callerId;
    }

    public List<String> getAllowedScopes() {
        return allowedScopes;
    }

    public boolean hasPIIAccess() {
        return hasPIIAccess;
    }

    /**
     * Masks a PII value if caller lacks pii:read scope.
     */
    public String maskIfNoAccess(String value) {
        if (!hasPIIAccess) {
            return value != null ? "***" : null;
        }
        return value;
    }
}
