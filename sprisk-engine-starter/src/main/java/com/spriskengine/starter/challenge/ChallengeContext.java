package com.spriskengine.starter.challenge;

import com.spriskengine.model.Decision;
import com.spriskengine.model.RiskResult;
import com.spriskengine.starter.rules.HardRuleEvaluator;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Context shared with {@link ChallengeHandler} implementations when a challenge decision is reached.
 */
public record ChallengeContext(
        RiskResult result,
        Decision decision,
        String reason,
        HardRuleEvaluator.HardRuleHit hardRuleHit,
        HttpServletRequest request,
        ChallengePolicy policy
)
{
}
