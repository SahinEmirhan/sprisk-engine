package io.github.sahinemirhan.starter.aop.internal;

/**
 * Internal helper describing whether execution should proceed after a risk decision.
 */
final class DecisionOutcome {

    private static final DecisionOutcome PROCEED = new DecisionOutcome(true, null);

    private final boolean proceed;
    private final Object result;

    private DecisionOutcome(boolean proceed, Object result) {
        this.proceed = proceed;
        this.result = result;
    }

    static DecisionOutcome proceed() {
        return PROCEED;
    }

    static DecisionOutcome stopWith(Object result) {
        return new DecisionOutcome(false, result);
    }

    boolean shouldProceed() {
        return proceed;
    }

    Object result() {
        return result;
    }
}
