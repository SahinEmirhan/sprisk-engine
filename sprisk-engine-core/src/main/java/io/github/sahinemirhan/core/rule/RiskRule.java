package io.github.sahinemirhan.core.rule;

import io.github.sahinemirhan.core.model.RiskContext;

public interface RiskRule {
    int evaluate(RiskContext ctx);
    String code();
}

