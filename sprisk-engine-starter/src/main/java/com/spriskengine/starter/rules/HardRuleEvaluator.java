package com.spriskengine.starter.rules;

import com.spriskengine.model.Decision;
import com.spriskengine.model.RiskResult;
import com.spriskengine.starter.config.SpriskProperties;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Evaluates hard rules configured in YAML. A hard rule can force a specific decision
 * (ALLOW, CHALLENGE, BLOCK) based on which risk rules fired in the current evaluation.
 */
public class HardRuleEvaluator {

    private final List<HardRule> hardRules;

    public HardRuleEvaluator(Map<String, SpriskProperties.HardRuleConfig> configs) {
        Map<String, SpriskProperties.HardRuleConfig> merged = new LinkedHashMap<>();
        merged.putAll(defaultHardRules());
        if (configs != null && !configs.isEmpty()) {
            merged.putAll(configs);
        }
        this.hardRules = merged.entrySet()
                .stream()
                .map(entry -> new HardRule(entry.getKey(), entry.getValue()))
                .toList();
    }

    public Optional<HardRuleHit> evaluate(RiskResult result) {
        if (result == null || hardRules.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Boolean> flags = normalizeRuleFlags(result.ruleFlags());
        for (HardRule hardRule : hardRules) {
            if (hardRule.matches(flags)) {
                return Optional.of(new HardRuleHit(hardRule.name(), hardRule.action()));
            }
        }
        return Optional.empty();
    }

    private Map<String, Boolean> normalizeRuleFlags(Map<String, Boolean> ruleFlags) {
        if (ruleFlags == null || ruleFlags.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Boolean> normalized = new LinkedHashMap<>();
        ruleFlags.forEach((code, value) -> normalized.put(
                normalizeKey(code),
                value != null && value
        ));
        return Collections.unmodifiableMap(normalized);
    }

    private static String normalizeKey(String key) {
        if (key == null) return "";
        return key.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }

    private Map<String, SpriskProperties.HardRuleConfig> defaultHardRules() {
        SpriskProperties.HardRuleConfig config = new SpriskProperties.HardRuleConfig();
        Map<String, Boolean> match = new LinkedHashMap<>();
        match.put("ip-velocity", false);
        match.put("user-velocity", true);
        config.setMatch(match);
        config.setAction(Decision.BLOCK);
        return Map.of("distributed-user-attack", config);
    }

    private static class HardRule {
        private final String name;
        private final Map<String, Boolean> match;
        private final Decision action;

        private HardRule(String name, SpriskProperties.HardRuleConfig source) {
            this.name = name;
            if (source == null) {
                this.match = Map.of();
                this.action = Decision.ALLOW;
            } else {
                this.match = source.getMatch().entrySet().stream()
                        .collect(Collectors.toMap(
                                entry -> normalizeKey(entry.getKey()),
                                entry -> Boolean.TRUE.equals(entry.getValue()),
                                (a, b) -> b,
                                LinkedHashMap::new
                        ));
                this.action = source.getAction();
            }
        }

        private boolean matches(Map<String, Boolean> triggeredFlags) {
            if (match.isEmpty()) {
                return false;
            }
            for (Map.Entry<String, Boolean> entry : match.entrySet()) {
                if (!triggeredFlags.containsKey(entry.getKey())) {
                    return false;
                }
                boolean expected = entry.getValue();
                boolean present = Boolean.TRUE.equals(triggeredFlags.get(entry.getKey()));
                if (expected != present) {
                    return false;
                }
            }
            return true;
        }

        private String name() {
            return name;
        }

        private Decision action() {
            return action;
        }
    }

    public record HardRuleHit(String ruleName, Decision decision) {}
}
