package io.github.sahinemirhan.starter.rules;



import io.github.sahinemirhan.core.model.RiskContext;
import io.github.sahinemirhan.core.rule.ConfigurableRiskRule;
import io.github.sahinemirhan.core.window.WindowManager;

import java.util.Collections;
import java.util.Map;

public class CredentialStuffingRule implements ConfigurableRiskRule {

    private final WindowManager windowManager;
    private final long windowSeconds;
    private final long maxDistinctUsers;
    private final int riskScore;
    private final boolean defaultEnabled;

    public CredentialStuffingRule(WindowManager windowManager,
                                  long windowSeconds,
                                  long maxDistinctUsers,
                                  int riskScore,
                                  boolean defaultEnabled) {
        this.windowManager = windowManager;
        this.windowSeconds = windowSeconds;
        this.maxDistinctUsers = maxDistinctUsers;
        this.riskScore = riskScore;
        this.defaultEnabled = defaultEnabled;
    }

    @Override
    public int evaluate(RiskContext ctx) {
        return evaluate(ctx, Collections.emptyMap());
    }

    @Override
    public int evaluate(RiskContext ctx, Map<String, String> properties) {
        String ip = ctx.ip();
        String userId = ctx.userId();
        System.out.println("[DEBUG] CREDENTIAL_STUFFING.evaluate - ip=" + ip + " userId=" + userId);
        if (ip == null || ip.isBlank() || userId == null || userId.isBlank()) return 0;
        long window = readLong(properties, windowSeconds, "windowseconds", "window");
        long max = readLong(properties, maxDistinctUsers, "maxdistinctusers");
        int score = readInt(properties, riskScore, "riskscore", "score");
        String setKey = "sprisk:loginfail:ip:users:" + ip;
        windowManager.addDistinctInWindow(setKey, userId, window);
        Long size = windowManager.distinctCount(setKey);
        System.out.println("[DEBUG] CREDENTIAL_STUFFING setKey=" + setKey + " size=" + size);
        return (size != null && size >= max) ? score : 0;
    }

    @Override
    public String code() {
        return "CREDENTIAL_STUFFING";
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
                System.out.println("[CredentialStuffingRule] Invalid long for " + key + ": " + value);
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
                System.out.println("[CredentialStuffingRule] Invalid int for " + key + ": " + value);
            }
        }
        return fallback;
    }
}

