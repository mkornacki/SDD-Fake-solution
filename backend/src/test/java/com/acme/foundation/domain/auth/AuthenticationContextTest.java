package com.acme.foundation.domain.auth;

import com.acme.foundation.domain.audit.AuthenticationContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationContextTest {

    @Test
    void constructor_setsAllFields() {
        Instant expiry = Instant.now().plusSeconds(3600);
        AuthenticationContext ctx = new AuthenticationContext(
                "user-123", List.of("read", "write"), "https://issuer.example.com", expiry, "client-1");

        assertThat(ctx.getSubjectId()).isEqualTo("user-123");
        assertThat(ctx.getScopes()).containsExactly("read", "write");
        assertThat(ctx.getIssuer()).isEqualTo("https://issuer.example.com");
        assertThat(ctx.getTokenExpiry()).isEqualTo(expiry);
        assertThat(ctx.getClientId()).isEqualTo("client-1");
    }

    @Test
    void hasScope_returnsTrue_whenScopePresent() {
        AuthenticationContext ctx = new AuthenticationContext(
                "user-1", List.of("read", "admin"), "https://issuer", Instant.now().plusSeconds(100), null);

        assertThat(ctx.hasScope("admin")).isTrue();
        assertThat(ctx.hasScope("write")).isFalse();
    }

    @Test
    void isExpired_returnsFalse_forFutureToken() {
        AuthenticationContext ctx = new AuthenticationContext(
                "user-1", List.of(), "https://issuer", Instant.now().plusSeconds(3600), null);

        assertThat(ctx.isExpired()).isFalse();
    }

    @Test
    void isExpired_returnsTrue_forPastToken() {
        AuthenticationContext ctx = new AuthenticationContext(
                "user-1", List.of(), "https://issuer", Instant.now().minusSeconds(1), null);

        assertThat(ctx.isExpired()).isTrue();
    }

    @Test
    void constructor_throwsOnBlankSubjectId() {
        assertThatThrownBy(() -> new AuthenticationContext(
                "", List.of(), "https://issuer", Instant.now().plusSeconds(100), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_throwsOnNullScopes() {
        assertThatThrownBy(() -> new AuthenticationContext(
                "user-1", null, "https://issuer", Instant.now().plusSeconds(100), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void scopes_areImmutable() {
        var mutableScopes = new java.util.ArrayList<>(List.of("read"));
        AuthenticationContext ctx = new AuthenticationContext(
                "user-1", mutableScopes, "https://issuer", Instant.now().plusSeconds(100), null);

        assertThatThrownBy(() -> ctx.getScopes().add("write"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
