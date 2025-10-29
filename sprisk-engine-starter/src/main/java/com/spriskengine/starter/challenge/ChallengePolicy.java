package com.spriskengine.starter.challenge;

import java.time.Duration;
import java.util.Objects;

/**
 * Immutable view of the active challenge/block policy.
 */
public record ChallengePolicy(
        Duration challengeTtl,
        Duration temporaryBlockTtl,
        Duration permanentBlockTtl,
        int escalationThreshold,
        boolean permanentBlockEnabled
) {

    public ChallengePolicy {
        Objects.requireNonNull(challengeTtl, "challengeTtl");
        Objects.requireNonNull(temporaryBlockTtl, "temporaryBlockTtl");
        Objects.requireNonNull(permanentBlockTtl, "permanentBlockTtl");
        if (escalationThreshold < 0) {
            throw new IllegalArgumentException("escalationThreshold must be >= 0");
        }
    }

    public Duration ttlFor(ChallengeOutcome.Status status) {
        return switch (status) {
            case ALLOW -> Duration.ZERO;
            case CHALLENGE -> challengeTtl;
            case BLOCK -> permanentBlockEnabled ? permanentBlockTtl : temporaryBlockTtl;
        };
    }
}

