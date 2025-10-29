package io.github.sahinemirhan.starter.challenge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.LongAdder;

/**
 * Built-in telemetry hook that logs, counts and publishes events for challenges.
 */
public class ChallengeTelemetry {

    private static final Logger log = LoggerFactory.getLogger(ChallengeTelemetry.class);

    private final ApplicationEventPublisher eventPublisher;
    private final LongAdder challengeCount = new LongAdder();

    public ChallengeTelemetry(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void record(ChallengeContext context) {
        challengeCount.increment();
        if (log.isWarnEnabled()) {
            Duration ttl = context.policy() != null ? context.policy().challengeTtl() : null;
            log.warn("[Sprisk] Challenge triggered at {}: reason={}, score={}, flags={}, ttl={}",
                    Instant.now(),
                    context.reason(),
                    context.result().score(),
                    context.result().ruleFlags(),
                    ttl != null ? ttl : "n/a");
        }
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new ChallengeEvent(context, challengeCount.sum()));
        }
    }

    public long totalChallenges() {
        return challengeCount.sum();
    }
}
