package com.spriskengine.starter.rules;

import com.spriskengine.core.model.RiskContext;
import com.spriskengine.core.rule.ConfigurableRiskRule;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Rule that produces a risk score when requests fall inside a configurable night-time window.
 */
public class NightTimeRule implements ConfigurableRiskRule {

    private final ZoneId zoneId;
    private final boolean defaultEnabled;
    private final int defaultStartHour;
    private final int defaultEndHour;
    private final int defaultRiskScore;

    public NightTimeRule(ZoneId zoneId,
                         boolean enabled,
                         int startHour,
                         int endHour,
                         int riskScore) {
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
        this.defaultEnabled = enabled;
        this.defaultStartHour = normalizeHour(startHour);
        this.defaultEndHour = normalizeHour(endHour);
        this.defaultRiskScore = Math.max(0, riskScore);
    }

    @Override
    public String code() {
        return "NIGHT_TIME";
    }

    @Override
    public boolean defaultEnabled() {
        return defaultEnabled;
    }

    @Override
    public int evaluate(RiskContext ctx) {
        return evaluate(ctx, Map.of());
    }

    @Override
    public int evaluate(RiskContext ctx, Map<String, String> properties) {
        boolean enabled = resolveBoolean(properties, "enabled", defaultEnabled);
        if (!enabled) {
            return 0;
        }
        int startHour = normalizeHour(resolveInt(properties, "startHour", defaultStartHour));
        int endHour = normalizeHour(resolveInt(properties, "endHour", defaultEndHour));
        int riskScore = Math.max(0, resolveInt(properties, "riskScore", defaultRiskScore));

        LocalDateTime timestamp = ctx != null ? ctx.timeStamp() : null;
        LocalTime requestTime = timestamp != null
                ? timestamp.toLocalTime()
                : LocalDateTime.now(zoneId).toLocalTime();

        if (isWithinWindow(requestTime, startHour, endHour)) {
            return riskScore;
        }
        return 0;
    }

    @Override
    public Map<String, Object> describeConfiguration() {
        Map<String, Object> map = new HashMap<>();
        map.put("enabled", defaultEnabled);
        map.put("startHour", defaultStartHour);
        map.put("endHour", defaultEndHour);
        map.put("riskScore", defaultRiskScore);
        map.put("zoneId", zoneId.getId());
        return map;
    }

    private static int normalizeHour(int hour) {
        if (hour < 0) return 0;
        if (hour > 23) return 23;
        return hour;
    }

    private boolean isWithinWindow(LocalTime time, int startHour, int endHour) {
        LocalTime start = LocalTime.of(startHour, 0);
        LocalTime end = LocalTime.of(endHour, 0);

        if (start.equals(end)) {
            // covering entire day
            return true;
        }
        if (start.isBefore(end)) {
            return !time.isBefore(start) && time.isBefore(end);
        }
        // window crosses midnight e.g. 22:00-04:00
        return !time.isBefore(start) || time.isBefore(end);
    }

    private boolean resolveBoolean(Map<String, String> props, String key, boolean fallback) {
        String value = find(props, key);
        if (value == null) {
            return fallback;
        }
        return Boolean.parseBoolean(value);
    }

    private int resolveInt(Map<String, String> props, String key, int fallback) {
        String value = find(props, key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String find(Map<String, String> props, String key) {
        if (props == null || props.isEmpty()) {
            return null;
        }
        String normalizedKey = key.toLowerCase();
        for (Map.Entry<String, String> entry : props.entrySet()) {
            if (entry.getKey() == null) continue;
            String candidate = entry.getKey().toLowerCase().replace("-", "").replace("_", "");
            String target = normalizedKey.replace("-", "").replace("_", "");
            if (candidate.equals(target)) {
                return entry.getValue();
            }
        }
        return null;
    }
}

