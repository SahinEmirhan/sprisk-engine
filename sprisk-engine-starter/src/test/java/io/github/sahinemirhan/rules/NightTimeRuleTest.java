package io.github.sahinemirhan.rules;

import io.github.sahinemirhan.core.model.RiskContext;
import io.github.sahinemirhan.starter.rules.NightTimeRule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NightTimeRuleTest {

    private static RiskContext contextAt(int hour) {
        return new RiskContext("action", "user", "ip",
                LocalDateTime.of(2025, 1, 1, hour, 0),
                Map.of());
    }

    @Test
    void returnsRiskScoreInsideWindow() {
        NightTimeRule rule = new NightTimeRule(ZoneId.of("UTC"), true, 2, 6, 20);
        int score = rule.evaluate(contextAt(3));
        assertEquals(20, score);
    }

    @Test
    void returnsZeroOutsideWindow() {
        NightTimeRule rule = new NightTimeRule(ZoneId.of("UTC"), true, 2, 6, 20);
        int score = rule.evaluate(contextAt(7));
        assertEquals(0, score);
    }

    @Test
    void honorsDisabledConfiguration() {
        NightTimeRule rule = new NightTimeRule(ZoneId.of("UTC"), false, 2, 6, 20);
        int score = rule.evaluate(contextAt(3));
        assertEquals(0, score);
    }

    @Test
    void supportsWrapAroundWindow() {
        NightTimeRule rule = new NightTimeRule(ZoneId.of("UTC"), true, 22, 4, 25);
        assertEquals(25, rule.evaluate(contextAt(23)));
        assertEquals(25, rule.evaluate(contextAt(2)));
        assertEquals(0, rule.evaluate(contextAt(12)));
    }

    @Test
    void propertyOverridesTakePrecedence() {
        NightTimeRule rule = new NightTimeRule(ZoneId.of("UTC"), true, 2, 6, 15);
        Map<String, String> overrides = Map.of(
                "startHour", "10",
                "endHour", "12",
                "riskScore", "45"
        );
        assertEquals(45, rule.evaluate(contextAt(10), overrides));
        assertEquals(0, rule.evaluate(contextAt(3), overrides));
    }

    @Test
    void propertyOverridesCanDisableRule() {
        NightTimeRule rule = new NightTimeRule(ZoneId.of("UTC"), true, 0, 6, 15);
        Map<String, String> overrides = Map.of("enabled", "false");
        assertEquals(0, rule.evaluate(contextAt(1), overrides));
    }
}

