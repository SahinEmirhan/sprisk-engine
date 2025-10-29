package io.github.sahinemirhan.starter.challenge;


import io.github.sahinemirhan.core.model.RiskResult;
import io.github.sahinemirhan.starter.rules.HardRuleEvaluator;

/**
 * Strategy abstraction that evaluates which {@link ChallengePolicy} should be active for a given decision.
 */
@FunctionalInterface
public interface ChallengePolicyStrategy {

    ChallengePolicy resolve(RiskResult result, HardRuleEvaluator.HardRuleHit hardRuleHit);
}


