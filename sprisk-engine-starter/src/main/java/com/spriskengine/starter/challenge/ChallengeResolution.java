package com.spriskengine.starter.challenge;

import java.util.Objects;

/**
 * Result returned by a {@link ChallengeHandler}.
 */
public final class ChallengeResolution {

    public enum Type {
        PROCEED,
        RETURN,
        THROW
    }

    private static final ChallengeResolution PROCEED = new ChallengeResolution(Type.PROCEED, null, null, null);

    private final Type type;
    private final Object returnValue;
    private final RuntimeException exception;
    private final ChallengeOutcome outcome;

    private ChallengeResolution(Type type,
                                Object returnValue,
                                RuntimeException exception,
                                ChallengeOutcome outcome) {
        this.type = Objects.requireNonNull(type, "type");
        this.returnValue = returnValue;
        this.exception = exception;
        this.outcome = outcome;
    }

    public static ChallengeResolution proceed() {
        return PROCEED;
    }

    public static ChallengeResolution proceed(ChallengeOutcome outcome) {
        if (outcome == null) {
            return PROCEED;
        }
        return new ChallengeResolution(Type.PROCEED, null, null, outcome);
    }

    public static ChallengeResolution returning(Object value) {
        return returning(value, null);
    }

    public static ChallengeResolution returning(Object value, ChallengeOutcome outcome) {
        return new ChallengeResolution(Type.RETURN, value, null, outcome);
    }

    public static ChallengeResolution throwing(RuntimeException exception) {
        return throwing(exception, null);
    }

    public static ChallengeResolution throwing(RuntimeException exception, ChallengeOutcome outcome) {
        return new ChallengeResolution(Type.THROW, null, Objects.requireNonNull(exception, "exception"), outcome);
    }

    public Type type() {
        return type;
    }

    public Object returnValue() {
        return returnValue;
    }

    public RuntimeException exception() {
        return exception;
    }

    public ChallengeOutcome outcome() {
        return outcome;
    }
}
