package io.github.sahinemirhan.config;

import io.github.sahinemirhan.starter.challenge.ChallengeContext;
import io.github.sahinemirhan.starter.challenge.ChallengeHandler;
import io.github.sahinemirhan.starter.challenge.ChallengeOutcome;
import io.github.sahinemirhan.starter.challenge.ChallengeResolution;
import io.github.sahinemirhan.starter.challenge.ChallengeTelemetry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * Example application handler that demonstrates customizing challenge responses.
 */
@Component
public class ExampleChallengeHandler implements ChallengeHandler {

    private final ChallengeTelemetry telemetry;

    public ExampleChallengeHandler(ChallengeTelemetry telemetry) {
        this.telemetry = telemetry;
    }

    @Override
    public ChallengeResolution handleChallenge(ChallengeContext context) {
        telemetry.record(context);
        Duration ttl = context.policy() != null ? context.policy().challengeTtl() : Duration.ofMinutes(5);
        Map<String, Object> body = Map.of(
                "status", "CHALLENGE",
                "reason", context.reason(),
                "score", context.result().score(),
                "totalChallenges", telemetry.totalChallenges(),
                "retryAfterSeconds", ttl.toSeconds()
        );
        var response = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        ChallengeOutcome outcome = ChallengeOutcome.challenge()
                .message(context.reason())
                .ttl(ttl)
                .metadata("hardRule", context.hardRuleHit() != null ? context.hardRuleHit().ruleName() : null)
                .build();
        return ChallengeResolution.returning(response, outcome);
    }
}
