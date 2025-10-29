package com.spriskengine.starter.autoconfig;

import com.spriskengine.engine.RuleEngine;
import com.spriskengine.model.DecisionProfile;
import com.spriskengine.rule.RiskRule;
import com.spriskengine.storage.RiskStorage;
import com.spriskengine.starter.aop.RiskAspect;
import com.spriskengine.starter.aop.internal.RiskConfigurationResolver;
import com.spriskengine.starter.aop.internal.RiskEvaluationProcessor;
import com.spriskengine.starter.aop.internal.RiskInvocationFactory;
import com.spriskengine.starter.aop.internal.RuleOverrideParser;
import com.spriskengine.starter.challenge.BlockHandler;
import com.spriskengine.starter.challenge.ChallengeHandler;
import com.spriskengine.starter.challenge.ChallengeOutcomeListener;
import com.spriskengine.starter.challenge.ChallengePolicyStrategy;
import com.spriskengine.starter.challenge.ChallengeTelemetry;
import com.spriskengine.starter.challenge.ExceptionThrowingBlockHandler;
import com.spriskengine.starter.challenge.ExceptionThrowingChallengeHandler;
import com.spriskengine.starter.challenge.PropertiesChallengePolicyStrategy;
import com.spriskengine.starter.config.ChallengePolicyProperties;
import com.spriskengine.starter.config.SpriskProperties;
import com.spriskengine.starter.config.rule.BruteForceRuleProperties;
import com.spriskengine.starter.config.rule.CredentialStuffingRuleProperties;
import com.spriskengine.starter.config.rule.IpVelocityRuleProperties;
import com.spriskengine.starter.config.rule.NightTimeRuleProperties;
import com.spriskengine.starter.config.rule.UserVelocityRuleProperties;
import com.spriskengine.starter.resolver.CompositeUserIdResolver;
import com.spriskengine.starter.resolver.RiskUserIdResolver;
import com.spriskengine.starter.rules.BruteForceRule;
import com.spriskengine.starter.rules.CredentialStuffingRule;
import com.spriskengine.starter.rules.HardRuleEvaluator;
import com.spriskengine.starter.rules.IpVelocityRule;
import com.spriskengine.starter.rules.NightTimeRule;
import com.spriskengine.starter.rules.UserVelocityRule;
import com.spriskengine.starter.storage.InMemoryRiskStorage;
import com.spriskengine.starter.storage.RedisRiskStorage;
import com.spriskengine.window.WindowManager;
import com.spriskengine.window.WindowStrategy;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@AutoConfiguration
@EnableConfigurationProperties({
        SpriskProperties.class,
        ChallengePolicyProperties.class,
        IpVelocityRuleProperties.class,
        UserVelocityRuleProperties.class,
        BruteForceRuleProperties.class,
        CredentialStuffingRuleProperties.class,
        NightTimeRuleProperties.class
})
public class SpriskAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class SpriskAopConfiguration {
    }

    @Bean
    public DecisionProfile decisionProfile(SpriskProperties props) {
        return new DecisionProfile(props.getChallengeThreshold(), props.getBlockThreshold());
    }

    @Bean
    public HardRuleEvaluator hardRuleEvaluator(SpriskProperties props) {
        return new HardRuleEvaluator(props.getHardRules());
    }

    @Bean
    public RuleOverrideParser ruleOverrideParser() {
        return new RuleOverrideParser();
    }

    @Bean
    public RiskConfigurationResolver riskConfigurationResolver(RuleOverrideParser parser) {
        return new RiskConfigurationResolver(parser);
    }

    @Bean
    public RiskInvocationFactory riskInvocationFactory(List<RiskUserIdResolver> resolvers) {
        return new RiskInvocationFactory(new DefaultParameterNameDiscoverer(), resolvers);
    }

    @Bean
    @ConditionalOnMissingBean
    public ChallengeTelemetry challengeTelemetry(ApplicationEventPublisher eventPublisher) {
        return new ChallengeTelemetry(eventPublisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public ChallengeHandler challengeHandler(ChallengeTelemetry telemetry) {
        return new ExceptionThrowingChallengeHandler(telemetry);
    }

    @Bean
    @ConditionalOnMissingBean
    public BlockHandler blockHandler() {
        return new ExceptionThrowingBlockHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public ChallengePolicyStrategy challengePolicyStrategy(ChallengePolicyProperties properties) {
        return new PropertiesChallengePolicyStrategy(properties);
    }

    @Bean
    public RiskEvaluationProcessor riskEvaluationProcessor(RuleEngine ruleEngine,
                                                           DecisionProfile decisionProfile,
                                                           HardRuleEvaluator hardRuleEvaluator,
                                                           ChallengeHandler challengeHandler,
                                                           BlockHandler blockHandler,
                                                           ChallengePolicyStrategy challengePolicyStrategy,
                                                           ObjectProvider<ChallengeOutcomeListener> outcomeListeners) {
        List<ChallengeOutcomeListener> listeners = outcomeListeners
                .orderedStream()
                .collect(Collectors.toList());
        return new RiskEvaluationProcessor(ruleEngine,
                decisionProfile,
                hardRuleEvaluator,
                challengeHandler,
                blockHandler,
                challengePolicyStrategy,
                listeners);
    }

    @Bean
    public RiskAspect riskAspect(SpriskProperties props,
                                 RiskConfigurationResolver resolver,
                                 RiskInvocationFactory invocationFactory,
                                 RiskEvaluationProcessor evaluationProcessor,
                                 ObjectProvider<HttpServletRequest> requestProvider) {
        return new RiskAspect(props, resolver, invocationFactory, evaluationProcessor, requestProvider);
    }

    @Bean
    @ConditionalOnMissingBean(RiskStorage.class)
    @ConditionalOnBean(StringRedisTemplate.class)
    public RiskStorage redisStorage(StringRedisTemplate redisTemplate) {
        try {
            redisTemplate.execute((RedisCallback<Void>) connection -> {
                connection.ping();
                return null;
            });
            System.out.println("[Sprisk] RedisStorage activated (Redis connection successful)");
            return new RedisRiskStorage(redisTemplate);
        } catch (Exception ex) {
            System.out.println("[Sprisk] Redis unavailable, falling back to InMemoryStorage");
            return new InMemoryRiskStorage();
        }
    }

    @Bean
    @ConditionalOnMissingBean(RiskStorage.class)
    public RiskStorage inMemoryStorage() {
        System.out.println("[Sprisk] InMemoryStorage fallback active");
        return new InMemoryRiskStorage();
    }

    @Bean
    public WindowManager windowManager(RiskStorage storage, SpriskProperties props) {
        WindowStrategy strategy = props.getWindowStrategy() != null ? props.getWindowStrategy() : WindowStrategy.SLIDING;
        System.out.println("[Sprisk] Window strategy = " + strategy);
        return new WindowManager(storage, strategy);
    }

    @Bean
    public NightTimeRule nightTimeRule(SpriskProperties props, NightTimeRuleProperties nightProps) {
        String tz = props.getTimezone();
        ZoneId zoneId = tz != null ? ZoneId.of(tz) : ZoneId.systemDefault();
        return new NightTimeRule(zoneId,
                nightProps.isEnabled(),
                nightProps.getStartHour(),
                nightProps.getEndHour(),
                nightProps.getRiskScore());
    }

    @Bean
    public IpVelocityRule ipVelocityRule(WindowManager windowManager, IpVelocityRuleProperties props) {
        return new IpVelocityRule(windowManager,
                props.getWindowSeconds(),
                props.getMaxPerWindow(),
                props.getRiskScore(),
                props.isEnabled());
    }

    @Bean
    public UserVelocityRule userVelocityRule(WindowManager windowManager, UserVelocityRuleProperties props) {
        return new UserVelocityRule(windowManager,
                props.getWindowSeconds(),
                props.getMaxPerWindow(),
                props.getRiskScore(),
                props.getExcludedActions(),
                props.isEnabled());
    }

    @Bean
    public BruteForceRule bruteForceRule(WindowManager windowManager, BruteForceRuleProperties props) {
        System.out.println("[Sprisk] BruteForceRule registered");
        return new BruteForceRule(windowManager,
                props.getWindowSeconds(),
                props.getMaxFail(),
                props.getRiskScore(),
                props.isEnabled());
    }

    @Bean
    public CredentialStuffingRule credentialStuffingRule(WindowManager windowManager, CredentialStuffingRuleProperties props) {
        System.out.println("[Sprisk] CredentialStuffingRule registered");
        return new CredentialStuffingRule(windowManager,
                props.getWindowSeconds(),
                props.getMaxDistinctUserCount(),
                props.getRiskScore(),
                props.isEnabled());
    }

    @Bean
    @ConditionalOnMissingBean(RiskUserIdResolver.class)
    public RiskUserIdResolver userIdResolver() {
        return CompositeUserIdResolver.builder()
                .attempt(CompositeUserIdResolver.fromRequestAttributes("sprisk.userId", "userId", "user-id"))
                .attempt(CompositeUserIdResolver.fromHeaders("X-User-Id", "X-USER-ID", "X_USER_ID"))
                .attempt(CompositeUserIdResolver.fromPrincipal(principal -> principal != null ? principal.getName() : null))
                .build();
    }

    @Bean
    public RuleEngine ruleEngine(List<RiskRule> rules) {
        rules.forEach(rule -> System.out.println("[Sprisk] Rule loaded: " + rule.code()));
        System.out.println("[Sprisk] Total rules loaded = " + rules.size());
        return new RuleEngine(rules);
    }
}
