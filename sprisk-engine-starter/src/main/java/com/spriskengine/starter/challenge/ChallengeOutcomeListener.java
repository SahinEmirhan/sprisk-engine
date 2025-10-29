package com.spriskengine.starter.challenge;

/**
 * Callback hook that allows applications to observe produced {@link ChallengeOutcome} instances.
 * <p>
 * Beans implementing this interface are automatically detected by the starter and invoked
 * after the {@link ChallengeHandler} returns a resolution.
 */
@FunctionalInterface
public interface ChallengeOutcomeListener {

    void onChallengeOutcome(ChallengeOutcome outcome, ChallengeContext context);
}

