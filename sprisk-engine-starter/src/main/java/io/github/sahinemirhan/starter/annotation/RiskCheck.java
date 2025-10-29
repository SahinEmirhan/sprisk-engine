package io.github.sahinemirhan.starter.annotation;


import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RiskCheck {

    /**
     * Optional action identifier to classify the invocation (e.g. LOGIN, READ, TRANSFER).
     */
    String action() default "";

    String userId() default "";

    String ip() default "";

    String[] attributes() default {};

    /**
     * Per-rule override entries, e.g. {"IP_VELOCITY.enabled=false", "USER_VELOCITY.maxPerWindow=15"}.
     */
    String[] ruleProperties() default {};

    /**
     * Evaluate risk rules before the annotated method executes.
     * Keeps backward compatibility by defaulting to true.
     * Automatically ignored when {@link #evaluateOnFailure()} is enabled.
     */
    boolean evaluateBefore() default true;

    /**
     * Evaluate risk rules only when the annotated method throws an exception.
     * Useful for login flows where failures should drive the counters.
     */
    boolean evaluateOnFailure() default false;

    /**
     * Evaluate risk rules after the annotated method completes successfully.
     * Handy for actions that should influence counters on success (e.g. transfer, password change).
     */
    boolean evaluateAfterSuccess() default false;

}
