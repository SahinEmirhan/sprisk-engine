package io.github.sahinemirhan.starter.autoconfig;

import io.github.sahinemirhan.core.engine.RuleEngine;
import io.github.sahinemirhan.core.model.DecisionProfile;
import io.github.sahinemirhan.core.rule.RiskRule;
import io.github.sahinemirhan.core.storage.RiskStorage;
import io.github.sahinemirhan.core.window.WindowManager;
import io.github.sahinemirhan.core.window.WindowStrategy;
import io.github.sahinemirhan.starter.aop.RiskAspect;
import io.github.sahinemirhan.starter.aop.internal.RiskConfigurationResolver;
import io.github.sahinemirhan.starter.aop.internal.RiskEvaluationProcessor;
import io.github.sahinemirhan.starter.aop.internal.RiskInvocationFactory;
import io.github.sahinemirhan.starter.aop.internal.RuleOverrideParser;
import io.github.sahinemirhan.starter.challenge.BlockHandler;
import io.github.sahinemirhan.starter.challenge.ChallengeHandler;
import io.github.sahinemirhan.starter.challenge.ChallengeOutcomeListener;
import io.github.sahinemirhan.starter.challenge.ChallengePolicyStrategy;
import io.github.sahinemirhan.starter.challenge.ChallengeTelemetry;
import io.github.sahinemirhan.starter.challenge.ExceptionThrowingBlockHandler;
import io.github.sahinemirhan.starter.challenge.ExceptionThrowingChallengeHandler;
import io.github.sahinemirhan.starter.challenge.PropertiesChallengePolicyStrategy;
import io.github.sahinemirhan.starter.config.ChallengePolicyProperties;
import io.github.sahinemirhan.starter.config.SpriskProperties;
import io.github.sahinemirhan.starter.config.rule.BruteForceRuleProperties;
import io.github.sahinemirhan.starter.config.rule.CredentialStuffingRuleProperties;
import io.github.sahinemirhan.starter.config.rule.IpVelocityRuleProperties;
import io.github.sahinemirhan.starter.config.rule.NightTimeRuleProperties;
import io.github.sahinemirhan.starter.config.rule.UserVelocityRuleProperties;
import io.github.sahinemirhan.starter.resolver.CompositeUserIdResolver;
import io.github.sahinemirhan.starter.resolver.RiskUserIdResolver;
import io.github.sahinemirhan.starter.rules.BruteForceRule;
import io.github.sahinemirhan.starter.rules.CredentialStuffingRule;
import io.github.sahinemirhan.starter.rules.HardRuleEvaluator;
import io.github.sahinemirhan.starter.rules.IpVelocityRule;
import io.github.sahinemirhan.starter.rules.NightTimeRule;
import io.github.sahinemirhan.starter.rules.UserVelocityRule;
import io.github.sahinemirhan.starter.storage.InMemoryRiskStorage;
import io.github.sahinemirhan.starter.storage.RedisRiskStorage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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
        return new RiskEvaluationProcessor(
                ruleEngine,
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

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
    static class RedisRiskStorageConfiguration {

        @Bean
        @ConditionalOnMissingBean(RiskStorage.class)
        @ConditionalOnBean(type = "org.springframework.data.redis.core.StringRedisTemplate")
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
    }
}

