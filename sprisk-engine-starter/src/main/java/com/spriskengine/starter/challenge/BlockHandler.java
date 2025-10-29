package com.spriskengine.starter.challenge;

/**
 * Strategy interface that allows applications to customize how block decisions are enforced.
 */
public interface BlockHandler {

    /**
     * Handle an incoming block decision.
     *
     * @param context block context
     * @return resolution describing the desired handling. Implementations can optionally attach
     * a {@link ChallengeOutcome} to propagate block metadata.
     */
    ChallengeResolution handleBlock(BlockContext context);
}

