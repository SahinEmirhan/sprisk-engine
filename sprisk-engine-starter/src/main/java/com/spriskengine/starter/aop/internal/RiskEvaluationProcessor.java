package com.spriskengine.starter.aop.internal;

import com.spriskengine.engine.RuleEngine;
import com.spriskengine.model.Decision;
import com.spriskengine.model.DecisionProfile;
import com.spriskengine.model.RiskResult;
import com.spriskengine.starter.challenge.BlockContext;
import com.spriskengine.starter.challenge.BlockHandler;
import com.spriskengine.starter.challenge.ChallengeContext;
import com.spriskengine.starter.challenge.ChallengeHandler;
import com.spriskengine.starter.challenge.ChallengeOutcome;
import com.spriskengine.starter.challenge.ChallengeOutcomeListener;
import com.spriskengine.starter.challenge.ChallengePolicy;
import com.spriskengine.starter.challenge.ChallengePolicyStrategy;
import com.spriskengine.starter.challenge.ChallengeResolution;
import com.spriskengine.starter.exception.SpriskBlockedException;
import com.spriskengine.starter.exception.SpriskChallengeException;
import com.spriskengine.starter.rules.HardRuleEvaluator;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.spriskengine.starter.aop.internal.RiskAttributes.RULE_FLAGS;
import static com.spriskengine.starter.aop.internal.RiskAttributes.RULE_FLAGS_STRING;

public class RiskEvaluationProcessor {

    private static final Logger log = LoggerFactory.getLogger(RiskEvaluationProcessor.class);

    private final RuleEngine ruleEngine;
    private final DecisionProfile decisionProfile;
    private final HardRuleEvaluator hardRuleEvaluator;
    private final ChallengeHandler challengeHandler;
    private final BlockHandler blockHandler;
    private final ChallengePolicyStrategy challengePolicyStrategy;
    private final List<ChallengeOutcomeListener> outcomeListeners;
    private static final ChallengePolicy DEFAULT_POLICY = new ChallengePolicy(
            Duration.ofMinutes(5),
            Duration.ofMinutes(15),
            Duration.ofDays(365),
            3,
            true
    );

    public RiskEvaluationProcessor(RuleEngine ruleEngine,
                                   DecisionProfile decisionProfile,
                                   HardRuleEvaluator hardRuleEvaluator,
                                   ChallengeHandler challengeHandler,
                                   BlockHandler blockHandler,
                                   ChallengePolicyStrategy challengePolicyStrategy,
                                   List<ChallengeOutcomeListener> outcomeListeners) {
        this.ruleEngine = ruleEngine;
        this.decisionProfile = decisionProfile;
        this.hardRuleEvaluator = hardRuleEvaluator;
        this.challengeHandler = challengeHandler;
        this.blockHandler = blockHandler;
        this.challengePolicyStrategy = challengePolicyStrategy;
        this.outcomeListeners = outcomeListeners != null ? List.copyOf(outcomeListeners) : List.of();
    }

    public Object evaluate(ProceedingJoinPoint joinPoint, RiskInvocation invocation) throws Throwable {
        if (invocation.evaluateBefore()) {
            RiskResult result = evaluateOnce(invocation);
            DecisionOutcome outcome = enforceDecision(result, invocation.request());
            if (!outcome.shouldProceed()) {
                return outcome.result();
            }
        }

        try {
            Object result = joinPoint.proceed();
            if (invocation.evaluateAfterSuccess()) {
                RiskResult afterResult = evaluateOnce(invocation);
                DecisionOutcome outcome = enforceDecision(afterResult, invocation.request());
                if (!outcome.shouldProceed()) {
                    return outcome.result();
                }
            }
            return result;
        } catch (Throwable ex) {
            if (invocation.evaluateOnFailure()) {
                RiskResult failureResult = evaluateOnce(invocation);
                DecisionOutcome outcome = enforceDecision(failureResult, invocation.request());
                if (!outcome.shouldProceed()) {
                    return outcome.result();
                }
            }
            throw ex;
        }
    }

    private RiskResult evaluateOnce(RiskInvocation invocation) {
        return ruleEngine.evaluate(
                invocation.contextSupplier().get(),
                invocation.overrides().enabled(),
                invocation.overrides().properties()
        );
    }

    private DecisionOutcome enforceDecision(RiskResult result, HttpServletRequest request) {
        publishRequestAttributes(result, request);
        HardRuleEvaluator.HardRuleHit hardRuleHit = hardRuleEvaluator != null
                ? hardRuleEvaluator.evaluate(result).orElse(null)
                : null;

        Decision decision = hardRuleHit != null
                ? hardRuleHit.decision()
                : decisionProfile.decide(result.score());

        String reason = buildReasonMessage(result, hardRuleHit);

        ChallengePolicy policy = challengePolicyStrategy != null
                ? challengePolicyStrategy.resolve(result, hardRuleHit)
                : DEFAULT_POLICY;

        switch (decision) {
            case BLOCK -> {
                BlockContext blockContext = new BlockContext(result, reason, hardRuleHit, request, policy);
                ChallengeResolution resolution = blockHandler != null
                        ? blockHandler.handleBlock(blockContext)
                        : null;
                if (resolution == null) {
                    throw new SpriskBlockedException("Request blocked. Reason: " + reason);
                }
                publishOutcome(resolution.outcome(), toOutcomeContext(blockContext, Decision.BLOCK));
                return switch (resolution.type()) {
                    case PROCEED -> DecisionOutcome.proceed();
                    case RETURN -> DecisionOutcome.stopWith(resolution.returnValue());
                    case THROW -> throw ensureBlockException(resolution, reason);
                };
            }
            case CHALLENGE -> {
                ChallengeContext context = new ChallengeContext(result, decision, reason, hardRuleHit, request, policy);
                ChallengeResolution resolution = challengeHandler != null
                        ? challengeHandler.handleChallenge(context)
                        : null;
                if (resolution == null) {
                    throw new SpriskChallengeException("Verification required: " + reason);
                }
                publishOutcome(resolution.outcome(), context);
                return switch (resolution.type()) {
                    case PROCEED -> DecisionOutcome.proceed();
                    case RETURN -> DecisionOutcome.stopWith(resolution.returnValue());
                    case THROW -> throw ensureException(resolution, reason);
                };
            }
            default -> {
            }
        }
        return DecisionOutcome.proceed();
    }

    private void publishRequestAttributes(RiskResult result, HttpServletRequest request) {
        if (request == null) return;
        Map<String, Boolean> ruleFlags = result.ruleFlags();
        request.setAttribute(RULE_FLAGS, ruleFlags);
        request.setAttribute(RULE_FLAGS_STRING, buildRuleFlagsDisplay(ruleFlags));
    }

    private String buildReasonMessage(RiskResult result, HardRuleEvaluator.HardRuleHit hardRuleHit) {
        String reasons = result.reasons() != null ? result.reasons().toString() : "[]";
        String states = buildRuleFlagsDisplay(result.ruleFlags());
        String base = reasons + " states=" + states;
        if (hardRuleHit == null) {
            return base;
        }
        return "[HardRule:" + hardRuleHit.ruleName() + " -> " + hardRuleHit.decision() + "] " + base;
    }

    private String buildRuleFlagsDisplay(Map<String, Boolean> flags) {
        if (flags == null || flags.isEmpty()) {
            return "{}";
        }
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Boolean> entry : flags.entrySet()) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append(toDisplayKey(entry.getKey()))
                    .append("=")
                    .append(entry.getValue());
        }
        builder.append("}");
        return builder.toString();
    }

    private String toDisplayKey(String code) {
        return code == null ? "" : code.toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private RuntimeException ensureException(ChallengeResolution resolution, String reason) {
        RuntimeException ex = resolution.exception();
        if (ex != null) {
            return ex;
        }
        return new SpriskChallengeException("Verification required: " + reason);
    }

    private RuntimeException ensureBlockException(ChallengeResolution resolution, String reason) {
        RuntimeException ex = resolution.exception();
        if (ex != null) {
            return ex;
        }
        return new SpriskBlockedException("Request blocked. Reason: " + reason);
    }

    private void publishOutcome(ChallengeOutcome outcome, ChallengeContext context) {
        if (outcome == null || outcomeListeners.isEmpty()) {
            return;
        }
        outcomeListeners.forEach(listener -> safeNotify(listener, outcome, context));
    }

    private void safeNotify(ChallengeOutcomeListener listener, ChallengeOutcome outcome, ChallengeContext context) {
        try {
            listener.onChallengeOutcome(outcome, context);
        } catch (RuntimeException ex) {
            if (log.isDebugEnabled()) {
                log.debug("[Sprisk] ChallengeOutcomeListener failure from {}: {}", listener.getClass().getName(), ex.getMessage(), ex);
            } else if (log.isWarnEnabled()) {
                log.warn("[Sprisk] ChallengeOutcomeListener failure from {}: {}", listener.getClass().getName(), ex.getMessage());
            }
        }
    }

    private ChallengeContext toOutcomeContext(BlockContext blockContext, Decision decision) {
        return new ChallengeContext(
                blockContext.result(),
                decision,
                blockContext.reason(),
                blockContext.hardRuleHit(),
                blockContext.request(),
                blockContext.policy()
        );
    }
}



