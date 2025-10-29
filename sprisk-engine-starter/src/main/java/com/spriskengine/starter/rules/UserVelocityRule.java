package com.spriskengine.starter.rules;

import com.spriskengine.model.RiskContext;
import com.spriskengine.rule.ConfigurableRiskRule;
import com.spriskengine.window.WindowManager;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserVelocityRule implements ConfigurableRiskRule {

    private final WindowManager windowManager;
    private final long windowSeconds;
    private final long maxPerWindow;
    private final int riskScore;
    private final boolean defaultEnabled;
    private final Set<String> baselineExcluded;

    public UserVelocityRule(WindowManager windowManager,
                            long windowSeconds,
                            long maxPerWindow,
                            int riskScore,
                            Set<String> excludedActions,
                            boolean defaultEnabled) {
        this.windowManager = windowManager;
        this.windowSeconds = windowSeconds;
        this.maxPerWindow = maxPerWindow;
        this.riskScore = riskScore;
        this.defaultEnabled = defaultEnabled;
        this.baselineExcluded = excludedActions == null ? Set.of() : Set.copyOf(excludedActions);
    }

    @Override
    public int evaluate(RiskContext ctx) {
        return evaluate(ctx, Collections.emptyMap());
    }

    @Override
    public int evaluate(RiskContext ctx, Map<String, String> properties) {
        String userId = ctx.userId();
        String action = ctx.action();
        System.out.println("[DEBUG] USER_VELOCITY.evaluate - action=" + action + " userId=" + userId);
        if (userId == null || userId.isBlank()) return 0;
        Set<String> effectiveExcluded = mergeExcluded(properties.get("excludedactions"));
        if (action != null && effectiveExcluded.contains(action)) {
            System.out.println("[DEBUG] USER_VELOCITY skipped for action=" + action);
            return 0;
        }
        long window = readLong(properties, windowSeconds, "windowseconds", "window");
        long max = readLong(properties, maxPerWindow, "maxperwindow");
        int score = readInt(properties, riskScore, "riskscore", "score");
        String key = "sprisk:velocity:user:" + userId;
        Long count = windowManager.incrementInWindow(key, window);
        System.out.println("[DEBUG] USER_VELOCITY key=" + key + " count=" + count);
        return (count != null && count > max) ? score : 0;
    }

    @Override
    public String code() {
        return "USER_VELOCITY";
    }

    @Override
    public boolean defaultEnabled() {
        return defaultEnabled;
    }

    private Set<String> mergeExcluded(String overrideValue) {
        if (overrideValue == null || overrideValue.isBlank()) {
            return baselineExcluded;
        }
        Set<String> overrideSet = Stream.of(overrideValue.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
        if (overrideSet.isEmpty()) {
            return baselineExcluded;
        }
        return overrideSet;
    }

    private long readLong(Map<String, String> props, long fallback, String... keys) {
        for (String key : keys) {
            String value = props.get(key);
            if (value == null || value.isBlank()) {
                continue;
            }
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ex) {
                System.out.println("[UserVelocityRule] Invalid long for " + key + ": " + value);
            }
        }
        return fallback;
    }

    private int readInt(Map<String, String> props, int fallback, String... keys) {
        for (String key : keys) {
            String value = props.get(key);
            if (value == null || value.isBlank()) {
                continue;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                System.out.println("[UserVelocityRule] Invalid int for " + key + ": " + value);
            }
        }
        return fallback;
    }
}




