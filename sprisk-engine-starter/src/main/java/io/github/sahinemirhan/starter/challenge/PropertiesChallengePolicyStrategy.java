package io.github.sahinemirhan.starter.challenge;


import io.github.sahinemirhan.core.model.RiskResult;
import io.github.sahinemirhan.starter.config.ChallengePolicyProperties;
import io.github.sahinemirhan.starter.rules.HardRuleEvaluator;

import java.util.Objects;

/**
 * Default implementation backed by {@link ChallengePolicyProperties}.
 */
public class PropertiesChallengePolicyStrategy implements ChallengePolicyStrategy {

    private final ChallengePolicyProperties properties;

    public PropertiesChallengePolicyStrategy(ChallengePolicyProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    public ChallengePolicy resolve(RiskResult result, HardRuleEvaluator.HardRuleHit hardRuleHit) {
        return new ChallengePolicy(
                properties.getChallengeTtl(),
                properties.getTemporaryBlockTtl(),
                properties.getPermanentBlockTtl(),
                properties.getEscalationThreshold(),
                properties.isPermanentBlockEnabled()
        );
    }
}


