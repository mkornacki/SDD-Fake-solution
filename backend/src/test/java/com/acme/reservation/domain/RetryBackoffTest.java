package com.acme.reservation.domain;

import com.acme.reservation.domain.replay.RetryBackoffCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("T022: Exponential backoff calculation")
class RetryBackoffTest {

    @Test
    @DisplayName("Backoff doubles each attempt: 1s, 2s, 4s")
    void exponentialBackoff_doubles() {
        Duration d1 = RetryBackoffCalculator.calculate(1, 1, 2.0, 60, false, new Random(1));
        Duration d2 = RetryBackoffCalculator.calculate(2, 1, 2.0, 60, false, new Random(1));
        Duration d3 = RetryBackoffCalculator.calculate(3, 1, 2.0, 60, false, new Random(1));

        assertThat(d1).isEqualTo(Duration.ofSeconds(1));
        assertThat(d2).isEqualTo(Duration.ofSeconds(2));
        assertThat(d3).isEqualTo(Duration.ofSeconds(4));
    }

    @Test
    @DisplayName("Backoff is capped at max delay (60s)")
    void exponentialBackoff_respectsCap() {
        Duration d = RetryBackoffCalculator.calculate(10, 1, 2.0, 60, false, new Random(1));
        assertThat(d).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("Jitter keeps delay within +/-20% bounds")
    void jitter_staysInRange() {
        Duration base = RetryBackoffCalculator.calculate(4, 1, 2.0, 60, false, new Random(1));
        Duration jittered = RetryBackoffCalculator.calculate(4, 1, 2.0, 60, true, new Random(7));

        long expected = base.toMillis();
        long min = (long) (expected * 0.8);
        long max = (long) (expected * 1.2);

        assertThat(jittered.toMillis()).isBetween(min, max);
    }
}
