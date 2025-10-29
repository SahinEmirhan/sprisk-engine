package io.github.sahinemirhan.core.model;

public class DecisionProfile {

    private final int challengeThreshold;
    private final int blockThreshold;

    public DecisionProfile(int challengeThreshold, int blockThreshold) {
        this.challengeThreshold = challengeThreshold;
        this.blockThreshold = blockThreshold;
    }

    public Decision decide(int score) {
        System.out.println("Decision score = " + score);
        if (score >= blockThreshold) return Decision.BLOCK;
        if (score >= challengeThreshold) return Decision.CHALLENGE;
        return Decision.ALLOW;
    }
}

