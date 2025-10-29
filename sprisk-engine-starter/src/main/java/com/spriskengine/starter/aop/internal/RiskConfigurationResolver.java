package com.spriskengine.starter.aop.internal;

import com.spriskengine.starter.annotation.RiskCheck;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.ArrayList;
import java.util.List;

public class RiskConfigurationResolver {

    private final RuleOverrideParser overrideParser;

    public RiskConfigurationResolver(RuleOverrideParser overrideParser) {
        this.overrideParser = overrideParser;
    }

    public RiskConfiguration resolve(ProceedingJoinPoint joinPoint, RiskCheck methodLevel, RiskCheck typeLevel) {
        RiskCheck method = methodLevel;
        RiskCheck type = typeLevel;
        if (method == null && type == null) {
            return null;
        }

        String action = firstNonBlank(method != null ? method.action() : null, type != null ? type.action() : null, "");
        String userExpr = firstNonBlank(method != null ? method.userId() : null, type != null ? type.userId() : null, "");
        String ipExpr = firstNonBlank(method != null ? method.ip() : null, type != null ? type.ip() : null, "");

        boolean evaluateBefore = method != null ? method.evaluateBefore()
                : type != null ? type.evaluateBefore() : true;
        boolean evaluateOnFailure = method != null ? method.evaluateOnFailure()
                : type != null && type.evaluateOnFailure();
        boolean evaluateAfterSuccess = method != null ? method.evaluateAfterSuccess()
                : type != null && type.evaluateAfterSuccess();

        String[] attributes = mergeAttributes(type, method);

        RuleOverrideSet typeOverrides = type != null ? overrideParser.parse(type.ruleProperties()) : RuleOverrideSet.empty();
        RuleOverrideSet methodOverrides = method != null ? overrideParser.parse(method.ruleProperties()) : RuleOverrideSet.empty();
        RuleOverrideSet overrides = typeOverrides.merge(methodOverrides);

        return new RiskConfiguration(
                action,
                userExpr,
                ipExpr,
                evaluateBefore,
                evaluateOnFailure,
                evaluateAfterSuccess,
                attributes,
                overrides
        );
    }

    private String[] mergeAttributes(RiskCheck type, RiskCheck method) {
        List<String> merged = new ArrayList<>();
        if (type != null) {
            for (String attr : type.attributes()) {
                if (attr != null && !attr.isBlank()) merged.add(attr);
            }
        }
        if (method != null) {
            for (String attr : method.attributes()) {
                if (attr != null && !attr.isBlank()) merged.add(attr);
            }
        }
        return merged.toArray(new String[0]);
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return "";
    }
}

