package com.spriskengine.starter.aop.internal;

import com.spriskengine.core.model.RiskContext;
import com.spriskengine.starter.resolver.RiskUserIdResolver;
import com.spriskengine.starter.resolver.UserIdResolverContext;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;
import org.springframework.core.OrderComparator;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static com.spriskengine.starter.aop.internal.RiskAttributes.RULE_ENABLED_OVERRIDES;
import static com.spriskengine.starter.aop.internal.RiskAttributes.RULE_OVERRIDES_LEGACY;
import static com.spriskengine.starter.aop.internal.RiskAttributes.RULE_PROPERTY_OVERRIDES;

public class RiskInvocationFactory {

    private final ExpressionParser expressionParser;
    private final ParameterNameDiscoverer parameterNameDiscoverer;
    private final List<RiskUserIdResolver> userIdResolvers;

    public RiskInvocationFactory(ParameterNameDiscoverer parameterNameDiscoverer,
                                 List<RiskUserIdResolver> userIdResolvers) {
        this.expressionParser = new SpelExpressionParser();
        this.parameterNameDiscoverer = parameterNameDiscoverer;
        this.userIdResolvers = userIdResolvers == null
                ? List.of()
                : userIdResolvers.stream().sorted(OrderComparator.INSTANCE).toList();
    }

    public RiskInvocation build(ProceedingJoinPoint joinPoint,
                                HttpServletRequest request,
                                RiskConfiguration configuration) {

        RuleOverrideSet overrides = configuration.overrides();
        Map<String, Object> attributes = buildAttributesMap(overrides);

        String userId = resolveUserId(joinPoint, request, configuration.userExpression());
        String ip = resolveIp(joinPoint, request, configuration.ipExpression());

        Supplier<RiskContext> contextSupplier = () -> new RiskContext(
                configuration.action(),
                userId,
                ip,
                LocalDateTime.now(),
                attributes
        );

        boolean evaluateBefore = configuration.evaluateBefore() && !configuration.evaluateOnFailure();

        return new RiskInvocation(
                contextSupplier,
                overrides,
                evaluateBefore,
                configuration.evaluateOnFailure(),
                configuration.evaluateAfterSuccess(),
                request
        );
    }

    private Map<String, Object> buildAttributesMap(RuleOverrideSet overrides) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        if (overrides != null && !overrides.isEmpty()) {
            attributes.put(RULE_ENABLED_OVERRIDES, overrides.enabled());
            attributes.put(RULE_PROPERTY_OVERRIDES, overrides.properties());
            attributes.put(RULE_OVERRIDES_LEGACY, overrides.enabled()); // backward compatibility
        }
        return Collections.unmodifiableMap(attributes);
    }

    private String resolveUserId(ProceedingJoinPoint joinPoint,
                                 HttpServletRequest request,
                                 String expression) {
        String fromExpression = evaluateExpression(expression, joinPoint, request);
        if (StringUtils.hasText(fromExpression)) {
            return fromExpression;
        }
        for (RiskUserIdResolver resolver : userIdResolvers) {
            String value = resolver.resolve(new UserIdResolverContext(joinPoint, request));
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String resolveIp(ProceedingJoinPoint joinPoint,
                             HttpServletRequest request,
                             String expression) {
        String explicit = evaluateExpression(expression, joinPoint, request);
        if (StringUtils.hasText(explicit)) {
            return explicit;
        }
        if (request != null) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(forwarded)) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return "unknown";
    }

    private String evaluateExpression(String expression,
                                      ProceedingJoinPoint joinPoint,
                                      HttpServletRequest request) {
        if (!StringUtils.hasText(expression)) {
            return null;
        }
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            Object[] args = joinPoint.getArgs();
            context.setVariable("args", args);

            for (int i = 0; i < args.length; i++) {
                context.setVariable("arg" + i, args[i]);
            }

            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = parameterNameDiscoverer.getParameterNames(signature.getMethod());
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    context.setVariable(parameterNames[i], i < args.length ? args[i] : null);
                }
            }

            if (request != null) {
                context.setVariable("request", request);
                context.setVariable("headers", buildHeaderMap(request));
                context.setVariable("session", request.getSession(false));
                context.setVariable("principal", request.getUserPrincipal());
            }

            addSecurityContext(context);

            Object result = expressionParser.parseExpression(expression).getValue(context);
            return result != null ? Objects.toString(result) : null;
        } catch (Exception ex) {
            System.out.println("[RiskInvocationFactory] SpEL evaluation failed for '" + expression + "': " + ex.getMessage());
            return null;
        }
    }

    private void addSecurityContext(StandardEvaluationContext context) {
        try {
            Class<?> holderClass = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            Object securityContext = holderClass.getMethod("getContext").invoke(null);
            context.setVariable("security", securityContext);
        } catch (ClassNotFoundException ignored) {
            // Spring Security not on classpath
        } catch (ReflectiveOperationException ex) {
            System.out.println("[RiskInvocationFactory] Unable to access SecurityContext: " + ex.getMessage());
        }
    }

    private Map<String, String> buildHeaderMap(HttpServletRequest request) {
        if (request == null) {
            return Map.of();
        }
        Map<String, String> headers = new LinkedHashMap<>();
        var headerNames = request.getHeaderNames();
        if (headerNames == null) {
            return Map.of();
        }
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }
}

