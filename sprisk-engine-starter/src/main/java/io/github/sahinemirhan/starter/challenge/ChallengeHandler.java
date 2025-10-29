package io.github.sahinemirhan.starter.challenge;

/**
 * Strategy interface that allows applications to customize how challenges are handled.
 */
public interface ChallengeHandler {

    /**
     * Handle an incoming challenge decision.
     *
     * @param context challenge context
     * @return resolution describing the desired handling. Implementations can optionally attach
     * a {@link ChallengeOutcome} via the {@link ChallengeResolution} helpers to communicate
     * extra context (status, ttl, metadata) to listeners.
     */
    ChallengeResolution handleChallenge(ChallengeContext context);
}
