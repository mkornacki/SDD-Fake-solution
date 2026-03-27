package com.acme.reservation.configuration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Micrometer metrics registration for reservation resilience operations.
 */
@Configuration
public class MetricsConfig {

    @Bean
    public ReservationMetrics reservationMetrics(MeterRegistry meterRegistry) {
        return new ReservationMetrics(meterRegistry);
    }

    public static final class ReservationMetrics {

        private final AtomicInteger queueDepth = new AtomicInteger();
        private final AtomicLong consumerLagSeconds = new AtomicLong();
        private final AtomicLong totalCreateRequests = new AtomicLong();
        private final AtomicLong idempotencyHits = new AtomicLong();
        private final Counter retryCounter;
        private final Counter dlqGrowthCounter;
        private final Counter createErrorCounter;
        private final Counter partnerErrorCounter;
        private final Timer createLatencyTimer;
        private final Timer partnerIntegrationLatencyTimer;

        ReservationMetrics(MeterRegistry meterRegistry) {
            Gauge.builder("reservation.queue.depth", queueDepth, AtomicInteger::get)
                    .description("Current pending reservation integration queue depth")
                    .register(meterRegistry);
            Gauge.builder("reservation.consumer.lag.seconds", consumerLagSeconds, AtomicLong::get)
                    .description("Age in seconds of the oldest pending integration task")
                    .register(meterRegistry);
            Gauge.builder("reservation.idempotency.hit.ratio", this, ReservationMetrics::idempotencyHitRatio)
                    .description("Ratio of idempotent replays to total create requests")
                    .register(meterRegistry);

            retryCounter = Counter.builder("reservation.retry.count")
                    .description("Number of transient retries scheduled for partner integration")
                    .register(meterRegistry);
            dlqGrowthCounter = Counter.builder("reservation.dlq.growth")
                    .description("Number of items routed to the reservation dead-letter queue")
                    .register(meterRegistry);
            createErrorCounter = Counter.builder("reservation.create.errors")
                    .description("Number of failed reservation create command executions")
                    .register(meterRegistry);
            partnerErrorCounter = Counter.builder("reservation.partner.integration.errors")
                    .description("Number of partner integration processing failures")
                    .register(meterRegistry);
            createLatencyTimer = Timer.builder("reservation.create.latency")
                    .description("Latency of reservation create command execution")
                    .publishPercentileHistogram()
                    .register(meterRegistry);
            partnerIntegrationLatencyTimer = Timer.builder("reservation.partner.integration.latency")
                    .description("Latency of partner integration worker processing")
                    .publishPercentileHistogram()
                    .register(meterRegistry);
        }

        public void incrementCreateRequests() {
            totalCreateRequests.incrementAndGet();
        }

        public void incrementIdempotencyHit() {
            idempotencyHits.incrementAndGet();
        }

        public void incrementRetryCount() {
            retryCounter.increment();
        }

        public void incrementDlqGrowth() {
            dlqGrowthCounter.increment();
        }

        public void incrementCreateErrors() {
            createErrorCounter.increment();
        }

        public void incrementPartnerErrors() {
            partnerErrorCounter.increment();
        }

        public void recordCreateLatency(Duration duration) {
            createLatencyTimer.record(duration);
        }

        public void recordPartnerIntegrationLatency(Duration duration) {
            partnerIntegrationLatencyTimer.record(duration);
        }

        public void updateQueueDepth(int depth) {
            queueDepth.set(Math.max(depth, 0));
        }

        public void updateConsumerLag(Duration lag) {
            consumerLagSeconds.set(Math.max(lag.getSeconds(), 0));
        }

        private double idempotencyHitRatio() {
            long total = totalCreateRequests.get();
            if (total == 0) {
                return 0.0d;
            }
            return (double) idempotencyHits.get() / total;
        }
    }
}