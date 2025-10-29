package io.github.sahinemirhan.starter.aop.internal;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class RuleOverrideParser {

    public RuleOverrideSet parse(String[] entries) {
        if (entries == null || entries.length == 0) {
            return RuleOverrideSet.empty();
        }

        Map<String, Boolean> enabled = new LinkedHashMap<>();
        Map<String, Map<String, String>> properties = new LinkedHashMap<>();

        for (String entry : entries) {
            if (entry == null) continue;
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;

            int dotIndex = trimmed.indexOf('.');
            int eqIndex = trimmed.indexOf('=');
            if (dotIndex <= 0 || eqIndex <= dotIndex + 1) {
                System.out.println("[RuleOverrideParser] Ignoring malformed entry: " + entry);
                continue;
            }

            String ruleCodeRaw = trimmed.substring(0, dotIndex);
            String property = trimmed.substring(dotIndex + 1, eqIndex).trim();
            String value = trimmed.substring(eqIndex + 1).trim();

            if (property.isEmpty()) {
                System.out.println("[RuleOverrideParser] Empty property name in entry: " + entry);
                continue;
            }

            String normalizedCode = normalizeCode(ruleCodeRaw);
            String normalizedProperty = property.toLowerCase(Locale.ROOT);

            if ("enabled".equals(normalizedProperty)) {
                enabled.put(normalizedCode, parseBoolean(value));
            } else {
                properties
                        .computeIfAbsent(normalizedCode, k -> new LinkedHashMap<>())
                        .put(normalizedProperty, value);
            }
        }

        return RuleOverrideSet.of(enabled, properties);
    }

    private boolean parseBoolean(String value) {
        if (value == null) return false;
        return Boolean.parseBoolean(value.trim());
    }

    private String normalizeCode(String code) {
        return code.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }
}

