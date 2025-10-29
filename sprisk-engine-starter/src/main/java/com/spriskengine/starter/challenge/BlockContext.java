package com.spriskengine.starter.challenge;

import com.spriskengine.model.RiskResult;
import com.spriskengine.starter.rules.HardRuleEvaluator;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Context shared with {@link BlockHandler} implementations when a block decision is reached.
 */
public record BlockContext(
        RiskResult result,
        String reason,
        HardRuleEvaluator.HardRuleHit hardRuleHit,
        HttpServletRequest request,
        ChallengePolicy policy
) {
}

