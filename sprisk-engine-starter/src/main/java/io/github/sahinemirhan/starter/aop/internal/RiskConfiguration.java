package io.github.sahinemirhan.starter.aop.internal;

public record RiskConfiguration(
        String action,
        String userExpression,
        String ipExpression,
        boolean evaluateBefore,
        boolean evaluateOnFailure,
        boolean evaluateAfterSuccess,
        String[] attributes,
        RuleOverrideSet overrides
) {
}

