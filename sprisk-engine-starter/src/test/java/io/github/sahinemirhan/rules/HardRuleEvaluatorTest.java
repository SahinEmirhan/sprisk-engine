package io.github.sahinemirhan.rules;


import io.github.sahinemirhan.core.model.Decision;
import io.github.sahinemirhan.core.model.RiskResult;
import io.github.sahinemirhan.starter.config.SpriskProperties;
import io.github.sahinemirhan.starter.rules.HardRuleEvaluator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HardRuleEvaluatorTest {

    @Test
    void usesDefaultDistributedUserAttackRuleWhenNoConfigProvided() {
        HardRuleEvaluator evaluator = new HardRuleEvaluator(Map.of());
        RiskResult result = new RiskResult(
                120,
                List.of(),
                Map.of(),
                Map.of(
                        "IP_VELOCITY", false,
                        "USER_VELOCITY", true
                )
        );

        Optional<HardRuleEvaluator.HardRuleHit> hit = evaluator.evaluate(result);

        assertTrue(hit.isPresent());
        assertEquals("distributed-user-attack", hit.get().ruleName());
        assertEquals(Decision.BLOCK, hit.get().decision());
    }

    @Test
    void allowsOverridingDefaultHardRule() {
        SpriskProperties.HardRuleConfig custom = new SpriskProperties.HardRuleConfig();
        custom.setMatch(Map.of(
                "ip-velocity", true,
                "user-velocity", true
        ));
        custom.setAction(Decision.CHALLENGE);

        HardRuleEvaluator evaluator = new HardRuleEvaluator(Map.of(
                "distributed-user-attack", custom
        ));

        RiskResult result = new RiskResult(
                200,
                List.of(),
                Map.of(),
                Map.of(
                        "IP_VELOCITY", true,
                        "USER_VELOCITY", true
                )
        );

        Optional<HardRuleEvaluator.HardRuleHit> hit = evaluator.evaluate(result);

        assertTrue(hit.isPresent());
        assertEquals(Decision.CHALLENGE, hit.get().decision());
    }
}


