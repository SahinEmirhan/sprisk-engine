package com.spriskengine.starter.rules;

import com.spriskengine.model.RiskContext;
import com.spriskengine.rule.ConfigurableRiskRule;
import com.spriskengine.window.WindowManager;

import java.util.Collections;
import java.util.Map;

/**
 * Generic brute-force detector that counts repeated attempts for the same principal (userId).
 * Typically paired with {@code evaluateOnFailure = true} so only failing requests contribute.
 */
public class BruteForceRule implements ConfigurableRiskRule {

    private final WindowManager windowManager;
    private final long windowSeconds;
    private final long maxAttempts;
    private final int riskScore;
    private final boolean defaultEnabled;

    public BruteForceRule(WindowManager windowManager,
                          long windowSeconds,
                          long maxAttempts,
                          int riskScore,
                          boolean defaultEnabled) {
        this.windowManager = windowManager;
        this.windowSeconds = windowSeconds;
        this.maxAttempts = maxAttempts;
        this.riskScore = riskScore;
        this.defaultEnabled = defaultEnabled;
    }

    @Override
    public int evaluate(RiskContext ctx) {
        return evaluate(ctx, Collections.emptyMap());
    }

    @Override
    public int evaluate(RiskContext ctx, Map<String, String> properties) {
        String userId = ctx.userId();
        if (userId == null || userId.isBlank()) {
            return 0;
        }
        long window = readLong(properties, windowSeconds, "windowseconds", "window");
        long max = readLong(properties, maxAttempts, "maxattempts", "maxfail");
        int score = readInt(properties, riskScore, "riskscore", "score");
        String key = "sprisk:bruteforce:user:" + userId;
        Long count = windowManager.incrementInWindow(key, window);
        System.out.println("[DEBUG] BRUTE_FORCE user=" + userId + " count=" + count);
        return (count != null && count > max) ? score : 0;
    }

    @Override
    public String code() {
        return "BRUTE_FORCE";
    }

    @Override
    public boolean defaultEnabled() {
        return defaultEnabled;
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
                System.out.println("[BruteForceRule] Invalid long for " + key + ": " + value);
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
                System.out.println("[BruteForceRule] Invalid int for " + key + ": " + value);
            }
        }
        return fallback;
    }
}

