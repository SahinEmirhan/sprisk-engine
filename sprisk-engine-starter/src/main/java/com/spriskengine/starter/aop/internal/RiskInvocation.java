package com.spriskengine.starter.aop.internal;

import com.spriskengine.model.RiskContext;
import jakarta.servlet.http.HttpServletRequest;

import java.util.function.Supplier;

public record RiskInvocation(
        Supplier<RiskContext> contextSupplier,
        RuleOverrideSet overrides,
        boolean evaluateBefore,
        boolean evaluateOnFailure,
        boolean evaluateAfterSuccess,
        HttpServletRequest request
) {
}
