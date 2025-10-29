package com.spriskengine.core.rule;

import com.spriskengine.core.model.RiskContext;

public interface RiskRule {
    int evaluate(RiskContext ctx);
    String code();
}

