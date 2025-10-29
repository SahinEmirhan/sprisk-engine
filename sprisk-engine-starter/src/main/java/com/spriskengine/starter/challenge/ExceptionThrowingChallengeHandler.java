package com.spriskengine.starter.challenge;

import com.spriskengine.starter.exception.SpriskChallengeException;

import java.time.Duration;

/**
 * Default {@link ChallengeHandler} that records telemetry and rethrows the challenge as an exception.
 */
public class ExceptionThrowingChallengeHandler implements ChallengeHandler {

    private final ChallengeTelemetry telemetry;

    public ExceptionThrowingChallengeHandler(ChallengeTelemetry telemetry) {
        this.telemetry = telemetry;
    }

    @Override
    public ChallengeResolution handleChallenge(ChallengeContext context) {
        if (telemetry != null) {
            telemetry.record(context);
        }
        ChallengeOutcome outcome = ChallengeOutcome.challenge()
                .message(context.reason())
                .ttl(Duration.ZERO)
                .metadata("score", context.result().score())
                .build();
        return ChallengeResolution.throwing(
                new SpriskChallengeException("Verification required: " + context.reason()),
                outcome
        );
    }
}
