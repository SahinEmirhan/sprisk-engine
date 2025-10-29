package com.spriskengine.starter.aop.internal;

import com.spriskengine.core.engine.RuleEngine;
import com.spriskengine.core.model.DecisionProfile;
import com.spriskengine.core.model.RiskContext;
import com.spriskengine.core.model.RiskResult;
import com.spriskengine.starter.challenge.BlockHandler;
import com.spriskengine.starter.challenge.ChallengeHandler;
import com.spriskengine.starter.challenge.ChallengeOutcome;
import com.spriskengine.starter.challenge.ChallengeOutcomeListener;
import com.spriskengine.starter.challenge.ChallengePolicy;
import com.spriskengine.starter.challenge.ChallengePolicyStrategy;
import com.spriskengine.starter.challenge.ChallengeResolution;
import com.spriskengine.starter.rules.HardRuleEvaluator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RiskEvaluationProcessorTest {

    @Test
    void returnsCustomResponseWhenChallengeHandlerProvidesReturnValue() throws Throwable {
        RuleEngine ruleEngine = mock(RuleEngine.class);
        DecisionProfile profile = new DecisionProfile(10, 100);
        HardRuleEvaluator hardRuleEvaluator = mock(HardRuleEvaluator.class);
        ChallengeHandler handler = mock(ChallengeHandler.class);
        BlockHandler blockHandler = mock(BlockHandler.class);
        ChallengePolicy policy = new ChallengePolicy(
                Duration.ofMinutes(2),
                Duration.ofMinutes(10),
                Duration.ofDays(1),
                3,
                true
        );
        ChallengePolicyStrategy strategy = (result, hit) -> policy;
        List<ChallengeOutcomeListener> listeners = List.of();
        RiskEvaluationProcessor processor = new RiskEvaluationProcessor(
                ruleEngine,
                profile,
                hardRuleEvaluator,
                handler,
                blockHandler,
                strategy,
                listeners
        );

        RiskResult riskResult = new RiskResult(20, List.of("TEST"), Map.of(), Map.of());
        when(ruleEngine.evaluate(any(), any(), any())).thenReturn(riskResult);
        when(hardRuleEvaluator.evaluate(any())).thenReturn(Optional.empty());
        when(handler.handleChallenge(any())).thenReturn(ChallengeResolution.returning("challenge-response"));

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        RiskInvocation invocation = new RiskInvocation(
                () -> new RiskContext("action", "user", "127.0.0.1", LocalDateTime.now(), Map.of()),
                RuleOverrideSet.empty(),
                true,
                false,
                false,
                null
        );

        Object outcome = processor.evaluate(joinPoint, invocation);

        assertEquals("challenge-response", outcome);
        verify(handler).handleChallenge(any());
        verifyNoInteractions(joinPoint);
    }

    @Test
    void usesBlockHandlerResolutionWhenBlocking() throws Throwable {
        RuleEngine ruleEngine = mock(RuleEngine.class);
        DecisionProfile profile = new DecisionProfile(10, 50);
        HardRuleEvaluator hardRuleEvaluator = mock(HardRuleEvaluator.class);
        ChallengeHandler challengeHandler = mock(ChallengeHandler.class);
        BlockHandler blockHandler = mock(BlockHandler.class);
        ChallengePolicy policy = new ChallengePolicy(
                Duration.ofMinutes(1),
                Duration.ofMinutes(30),
                Duration.ofDays(1),
                3,
                true
        );
        ChallengePolicyStrategy strategy = (result, hit) -> policy;
        List<ChallengeOutcomeListener> listeners = List.of();
        RiskEvaluationProcessor processor = new RiskEvaluationProcessor(
                ruleEngine,
                profile,
                hardRuleEvaluator,
                challengeHandler,
                blockHandler,
                strategy,
                listeners
        );

        RiskResult riskResult = new RiskResult(200, List.of("BLOCKED"), Map.of(), Map.of());
        when(ruleEngine.evaluate(any(), any(), any())).thenReturn(riskResult);
        when(hardRuleEvaluator.evaluate(any())).thenReturn(Optional.empty());
        when(blockHandler.handleBlock(any())).thenReturn(ChallengeResolution.returning("blocked-response"));

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        RiskInvocation invocation = new RiskInvocation(
                () -> new RiskContext("action", "user", "127.0.0.1", LocalDateTime.now(), Map.of()),
                RuleOverrideSet.empty(),
                true,
                false,
                false,
                null
        );

        Object outcome = processor.evaluate(joinPoint, invocation);

        assertEquals("blocked-response", outcome);
        verify(blockHandler).handleBlock(any());
        verifyNoInteractions(challengeHandler);
        verifyNoInteractions(joinPoint);
    }

    @Test
    void notifiesOutcomeListenersOnChallenge() throws Throwable {
        RuleEngine ruleEngine = mock(RuleEngine.class);
        DecisionProfile profile = new DecisionProfile(10, 200);
        HardRuleEvaluator hardRuleEvaluator = mock(HardRuleEvaluator.class);
        BlockHandler blockHandler = mock(BlockHandler.class);
        AtomicReference<com.spriskengine.starter.challenge.ChallengeOutcome> observed = new AtomicReference<>();
        ChallengeOutcomeListener listener = (outcome, context) -> observed.set(outcome);
        ChallengePolicy policy = new ChallengePolicy(
                Duration.ofMinutes(5),
                Duration.ofMinutes(15),
                Duration.ofDays(1),
                3,
                false
        );
        ChallengePolicyStrategy strategy = (result, hit) -> policy;
        ChallengeHandler handler = context -> ChallengeResolution.proceed(
                com.spriskengine.starter.challenge.ChallengeOutcome.challenge().message("verify").build());
        RiskEvaluationProcessor processor = new RiskEvaluationProcessor(
                ruleEngine,
                profile,
                hardRuleEvaluator,
                handler,
                blockHandler,
                strategy,
                List.of(listener)
        );

        RiskResult riskResult = new RiskResult(80, List.of("CHALLENGE"), Map.of(), Map.of());
        when(ruleEngine.evaluate(any(), any(), any())).thenReturn(riskResult);
        when(hardRuleEvaluator.evaluate(any())).thenReturn(Optional.empty());

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn("ok");
        RiskInvocation invocation = new RiskInvocation(
                () -> new RiskContext("action", "user", "127.0.0.1", LocalDateTime.now(), Map.of()),
                RuleOverrideSet.empty(),
                true,
                false,
                false,
                null
        );

        Object result = processor.evaluate(joinPoint, invocation);

        assertEquals("ok", result);
        assertSame(observed.get().status(), ChallengeOutcome.Status.CHALLENGE);
    }

}

