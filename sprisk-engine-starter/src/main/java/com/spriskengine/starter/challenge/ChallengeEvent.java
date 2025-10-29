package com.spriskengine.starter.challenge;

/**
 * Application event published whenever a challenge is triggered.
 *
 * @param context        full challenge context
 * @param totalChallenges running total of challenges observed since application start
 */
public record ChallengeEvent(ChallengeContext context, long totalChallenges) {
}
