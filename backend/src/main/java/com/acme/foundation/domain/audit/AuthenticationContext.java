package com.acme.foundation.domain.audit;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Runtime representation of a validated caller identity.
 * Derived from the JWT bearer token — not persisted.
 */
public final class AuthenticationContext {

    private final String subjectId;
    private final List<String> scopes;
    private final String issuer;
    private final Instant tokenExpiry;
    private final String clientId;

    public AuthenticationContext(
            String subjectId,
            List<String> scopes,
            String issuer,
            Instant tokenExpiry,
            String clientId) {

        if (subjectId == null || subjectId.isBlank()) {
            throw new IllegalArgumentException("subjectId must not be blank");
        }
        if (scopes == null) {
            throw new IllegalArgumentException("scopes must not be null");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("issuer must not be blank");
        }
        if (tokenExpiry == null) {
            throw new IllegalArgumentException("tokenExpiry must not be null");
        }

        this.subjectId = subjectId;
        this.scopes = Collections.unmodifiableList(scopes);
        this.issuer = issuer;
        this.tokenExpiry = tokenExpiry;
        this.clientId = clientId;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public String getIssuer() {
        return issuer;
    }

    public Instant getTokenExpiry() {
        return tokenExpiry;
    }

    public String getClientId() {
        return clientId;
    }

    public boolean hasScope(String requiredScope) {
        return scopes.contains(requiredScope);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(tokenExpiry);
    }
}
