package com.acme.reservation.domain.replay;

import java.time.Duration;
import java.util.Random;

/**
 * Calculates bounded exponential retry delays with optional jitter.
 */
public final class RetryBackoffCalculator {

    private static final double DEFAULT_JITTER_RATIO = 0.20;

    private RetryBackoffCalculator() {
    }

    public static Duration calculate(
            int attempt,
            long baseSeconds,
            double multiplier,
            long maxSeconds,
            boolean jitter) {
        return calculate(attempt, baseSeconds, multiplier, maxSeconds, jitter, new Random());
    }

    public static Duration calculate(
            int attempt,
            long baseSeconds,
            double multiplier,
            long maxSeconds,
            boolean jitter,
            Random random) {
        if (attempt <= 0) {
            throw new IllegalArgumentException("attempt must be >= 1");
        }
        if (baseSeconds <= 0 || maxSeconds <= 0) {
            throw new IllegalArgumentException("baseSeconds and maxSeconds must be > 0");
        }
        if (multiplier < 1.0) {
            throw new IllegalArgumentException("multiplier must be >= 1.0");
        }

        double raw = baseSeconds * Math.pow(multiplier, attempt - 1);
        long bounded = Math.min((long) Math.floor(raw), maxSeconds);

        if (!jitter) {
            return Duration.ofSeconds(bounded);
        }

        long millis = Duration.ofSeconds(bounded).toMillis();
        double factor = 1.0 + ((random.nextDouble() * 2.0 - 1.0) * DEFAULT_JITTER_RATIO);
        long jitteredMillis = Math.max(1L, (long) Math.floor(millis * factor));
        return Duration.ofMillis(jitteredMillis);
    }
}
