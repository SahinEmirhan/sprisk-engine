package io.github.sahinemirhan.core.engine;

import io.github.sahinemirhan.core.model.RiskContext;
import io.github.sahinemirhan.core.model.RiskResult;
import io.github.sahinemirhan.core.rule.ConfigurableRiskRule;
import io.github.sahinemirhan.core.rule.RiskRule;
import io.github.sahinemirhan.core.rule.ToggleableRiskRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class RuleEngine {

    private final List<RiskRule> rules;

    public RuleEngine(List<RiskRule> rules) {
        this.rules = rules == null ? List.of() : List.copyOf(rules);
    }

    /**
     * Evaluate all rules using the provided overrides.
     *
     * @param ctx               risk context supplier
     * @param enabledOverrides  normalized rule-code -> enabled flag overrides
     * @param propertyOverrides normalized rule-code -> property map overrides
     */
    public RiskResult evaluate(RiskContext ctx,
                               Map<String, Boolean> enabledOverrides,
                               Map<String, Map<String, String>> propertyOverrides) {
        Map<String, Boolean> overrideMap = enabledOverrides == null ? Collections.emptyMap() : enabledOverrides;
        Map<String, Map<String, String>> propertyMap = propertyOverrides == null ? Collections.emptyMap() : propertyOverrides;

        int totalScore = 0;
        List<String> hits = new ArrayList<>();
        Map<String, Integer> ruleScores = new LinkedHashMap<>();
        Map<String, Boolean> ruleFlags = new LinkedHashMap<>();

        for (RiskRule rule : rules) {
            String code = Objects.requireNonNull(rule.code(), "rule.code()");
            String normalizedCode = normalize(code);

            boolean enabled = isEnabled(rule, overrideMap, normalizedCode);
            if (!enabled) {
                ruleScores.put(code, 0);
                ruleFlags.put(code, false);
                continue;
            }

            int value;
            if (rule instanceof ConfigurableRiskRule configurable) {
                Map<String, String> properties = propertyMap.getOrDefault(normalizedCode, Collections.emptyMap());
                value = configurable.evaluate(ctx, properties);
            } else {
                value = rule.evaluate(ctx);
            }
            ruleScores.put(code, value);
            boolean triggered = value > 0;
            ruleFlags.put(code, triggered);
            if (triggered) {
                hits.add(code + ":" + value);
            }
            totalScore += value;
        }

        return new RiskResult(totalScore, hits, ruleScores, ruleFlags);
    }

    private boolean isEnabled(RiskRule rule, Map<String, Boolean> overrides, String normalizedCode) {
        Boolean override = overrides.get(normalizedCode);
        if (override != null) {
            return override;
        }
        if (rule instanceof ToggleableRiskRule toggleable) {
            return toggleable.defaultEnabled();
        }
        return true;
    }

    private String normalize(String code) {
        return code == null ? "" : code.replace('-', '_').toUpperCase(Locale.ROOT);
    }
}
