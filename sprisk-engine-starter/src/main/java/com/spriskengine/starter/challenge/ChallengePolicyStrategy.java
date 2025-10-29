package com.spriskengine.starter.challenge;

import com.spriskengine.core.model.RiskResult;
import com.spriskengine.starter.rules.HardRuleEvaluator;

/**
 * Strategy abstraction that evaluates which {@link ChallengePolicy} should be active for a given decision.
 */
@FunctionalInterface
public interface ChallengePolicyStrategy {

    ChallengePolicy resolve(RiskResult result, HardRuleEvaluator.HardRuleHit hardRuleHit);
}


