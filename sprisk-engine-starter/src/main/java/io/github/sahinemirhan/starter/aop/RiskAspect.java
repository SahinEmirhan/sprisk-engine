package io.github.sahinemirhan.starter.aop;

import io.github.sahinemirhan.starter.annotation.RiskCheck;
import io.github.sahinemirhan.starter.aop.internal.RiskConfiguration;
import io.github.sahinemirhan.starter.aop.internal.RiskConfigurationResolver;
import io.github.sahinemirhan.starter.aop.internal.RiskEvaluationProcessor;
import io.github.sahinemirhan.starter.aop.internal.RiskInvocation;
import io.github.sahinemirhan.starter.aop.internal.RiskInvocationFactory;
import io.github.sahinemirhan.starter.config.SpriskProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotationUtils;
import java.lang.reflect.Method;

import static io.github.sahinemirhan.starter.aop.internal.RiskAttributes.RULE_ENABLED_OVERRIDES;
import static io.github.sahinemirhan.starter.aop.internal.RiskAttributes.RULE_OVERRIDES_LEGACY;
import static io.github.sahinemirhan.starter.aop.internal.RiskAttributes.RULE_PROPERTY_OVERRIDES;

@Aspect
public class RiskAspect {

    private final SpriskProperties properties;
    private final RiskConfigurationResolver configurationResolver;
    private final RiskInvocationFactory invocationFactory;
    private final RiskEvaluationProcessor evaluationProcessor;
    private final ObjectProvider<HttpServletRequest> requestProvider;

    public RiskAspect(SpriskProperties properties,
                      RiskConfigurationResolver configurationResolver,
                      RiskInvocationFactory invocationFactory,
                      RiskEvaluationProcessor evaluationProcessor,
                      ObjectProvider<HttpServletRequest> requestProvider) {
        this.properties = properties;
        this.configurationResolver = configurationResolver;
        this.invocationFactory = invocationFactory;
        this.evaluationProcessor = evaluationProcessor;
        this.requestProvider = requestProvider;
        System.out.println("[Sprisk] RiskAspect initialized");
    }

    @Pointcut("@within(io.github.sahinemirhan.starter.annotation.RiskCheck) || @annotation(io.github.sahinemirhan.starter.annotation.RiskCheck)")
    public void riskAnnotated() {
        // Pointcut marker
    }

    @Around("riskAnnotated()")
    public Object handleRisk(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }

        RiskCheck methodLevelRisk = resolveMethodLevelAnnotation(joinPoint);
        RiskCheck typeLevelRisk = resolveTypeLevelAnnotation(joinPoint);

        RiskConfiguration configuration = configurationResolver.resolve(joinPoint, methodLevelRisk, typeLevelRisk);
        if (configuration == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest currentRequest = requestProvider.getIfAvailable();
        RiskInvocation invocation = invocationFactory.build(joinPoint, currentRequest, configuration);
        publishOverrideAttributes(invocation);
        return evaluationProcessor.evaluate(joinPoint, invocation);
    }

    private RiskCheck resolveMethodLevelAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> targetClass = joinPoint.getTarget() != null ? joinPoint.getTarget().getClass() : signature.getDeclaringType();
        if (targetClass == null) {
            return AnnotationUtils.findAnnotation(signature.getMethod(), RiskCheck.class);
        }
        Method method = AopUtils.getMostSpecificMethod(signature.getMethod(), targetClass);
        return AnnotationUtils.findAnnotation(method, RiskCheck.class);
    }

    private RiskCheck resolveTypeLevelAnnotation(ProceedingJoinPoint joinPoint) {
        Class<?> targetClass = joinPoint.getTarget() != null ? joinPoint.getTarget().getClass() : null;
        if (targetClass == null) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            targetClass = signature.getDeclaringType();
        }
        return targetClass != null ? AnnotationUtils.findAnnotation(targetClass, RiskCheck.class) : null;
    }

    private void publishOverrideAttributes(RiskInvocation invocation) {
        HttpServletRequest currentRequest = invocation.request();
        if (currentRequest == null || invocation.overrides().isEmpty()) {
            return;
        }
        currentRequest.setAttribute(RULE_ENABLED_OVERRIDES, invocation.overrides().enabled());
        currentRequest.setAttribute(RULE_PROPERTY_OVERRIDES, invocation.overrides().properties());
        currentRequest.setAttribute(RULE_OVERRIDES_LEGACY, invocation.overrides().enabled());
    }
}
