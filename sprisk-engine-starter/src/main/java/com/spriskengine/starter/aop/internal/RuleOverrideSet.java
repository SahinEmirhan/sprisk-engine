package com.spriskengine.starter.aop.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RuleOverrideSet {

    private final Map<String, Boolean> enabled;
    private final Map<String, Map<String, String>> properties;

    private RuleOverrideSet(Map<String, Boolean> enabled, Map<String, Map<String, String>> properties) {
        this.enabled = Collections.unmodifiableMap(enabled);
        Map<String, Map<String, String>> tmp = new LinkedHashMap<>();
        properties.forEach((code, map) -> tmp.put(code, Collections.unmodifiableMap(map)));
        this.properties = Collections.unmodifiableMap(tmp);
    }

    public static RuleOverrideSet empty() {
        return new RuleOverrideSet(new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    public static RuleOverrideSet of(Map<String, Boolean> enabled, Map<String, Map<String, String>> properties) {
        Map<String, Boolean> enabledCopy = new LinkedHashMap<>(enabled);
        Map<String, Map<String, String>> propertiesCopy = new LinkedHashMap<>();
        properties.forEach((code, map) -> propertiesCopy.put(code, new LinkedHashMap<>(map)));
        return new RuleOverrideSet(enabledCopy, propertiesCopy);
    }

    public Map<String, Boolean> enabled() {
        return enabled;
    }

    public Map<String, Map<String, String>> properties() {
        return properties;
    }

    public boolean isEmpty() {
        return enabled.isEmpty() && properties.isEmpty();
    }

    public RuleOverrideSet merge(RuleOverrideSet other) {
        if (other == null || other.isEmpty()) {
            return this;
        }
        Map<String, Boolean> enabledMerged = new LinkedHashMap<>(this.enabled);
        enabledMerged.putAll(other.enabled);

        Map<String, Map<String, String>> propertiesMerged = new LinkedHashMap<>();
        this.properties.forEach((code, map) -> propertiesMerged.put(code, new LinkedHashMap<>(map)));
        other.properties.forEach((code, map) ->
                propertiesMerged.computeIfAbsent(code, k -> new LinkedHashMap<>()).putAll(map)
        );

        return new RuleOverrideSet(enabledMerged, propertiesMerged);
    }
}

