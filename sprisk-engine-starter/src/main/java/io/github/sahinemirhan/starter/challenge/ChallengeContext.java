package io.github.sahinemirhan.starter.challenge;


import io.github.sahinemirhan.core.model.Decision;
import io.github.sahinemirhan.core.model.RiskResult;
import io.github.sahinemirhan.starter.rules.HardRuleEvaluator;
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

