package com.spriskengine.core.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record RiskResult(
        int score,
        List<String> reasons,
        Map<String, Integer> ruleScores,
        Map<String, Boolean> ruleFlags
) {

    public RiskResult {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
        ruleScores = ruleScores == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(ruleScores));
        ruleFlags = ruleFlags == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(ruleFlags));
    }
}
