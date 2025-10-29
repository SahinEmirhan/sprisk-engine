package io.github.sahinemirhan.core.rule;

public interface ToggleableRiskRule extends RiskRule {
    /**
     * Whether the rule should be considered enabled when no overrides are provided.
     */
    boolean defaultEnabled();
}

