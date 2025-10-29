package io.github.sahinemirhan.starter.challenge;


import io.github.sahinemirhan.core.model.RiskResult;
import io.github.sahinemirhan.starter.rules.HardRuleEvaluator;
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


