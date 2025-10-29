package com.spriskengine.core.rule;

import com.spriskengine.core.model.RiskContext;

import java.util.Collections;
import java.util.Map;

public interface ConfigurableRiskRule extends ToggleableRiskRule {

    default int evaluate(RiskContext ctx, Map<String, String> properties) {
        return evaluate(ctx);
    }

    default Map<String, Object> describeConfiguration() {
        return Collections.emptyMap();
    }
}

