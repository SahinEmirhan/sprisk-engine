# Sprisk Engine Developer Guide

Sprisk Engine is a lightweight and extensible risk scoring engine for Spring Boot, designed to detect suspicious login behavior such as brute force, credential stuffing, and velocity anomalies.

This guide explains how the Sprisk Engine modules work together, how to integrate the Spring Boot starter into your applications, and where you can customise behaviour. It is intended both for application teams that consume the starter and for engineers extending the engine itself.

---


## 0. Quick Start

1. **Add the dependency**

   ```kts
   repositories {
       mavenCentral()
   }

   dependencies {
       implementation("io.github.sahinemirhan:sprisk-engine-starter:0.0.2")
   }
   ```

   
2. **Annotate the endpoint** – decorate the HTTP or service method that should trigger a risk evaluation:

   ```java
   @GetMapping("/transfer")
   @RiskCheck(action = "TRANSFER", evaluateOnFailure = true)
   public ResponseEntity<?> transfer(...) { ... }
   ```

   The `action` attribute becomes part of the risk profile and helps with reporting and rule configuration.


3. **Provide a user identifier** – Sprisk requires a unique user identifier for every risk evaluation. Reference the identifier with the SpEL expression on `@RiskCheck` either at controller or service level:

   ```java
   // From a header
   @RiskCheck(userId = "#headers['X-User-Id']")
   
   // From a request parameter
   @GetMapping("/test")
   @RiskCheck(userId = "#id") //or @RiskCheck(userId = "#request.getParameter('name')")
   public String getUser(@RequestParam String id){
      return id;
   };
   
   // From a path variable
   @GetMapping("/test/{id}")
   @RiskCheck(userId = "#id") //or @RiskCheck(userId = "#pathVariables['id']")
   public String getUser(@PathVariable String id) {
      return id;
   }
   
   // From a request attribute set by a filter
   @RiskCheck(userId = "#request.getAttribute('sprisk.userId')")
   
   // From the Spring Security principal
   @RiskCheck(userId = "#request.userPrincipal?.name")
   ```


If you want to place the user id on the request yourself, register a simple filter:

   ```java
   @Component
   class UserAttributeFilter extends OncePerRequestFilter {
       @Override
       protected void doFilterInternal(HttpServletRequest request,
                                       HttpServletResponse response,
                                       FilterChain chain) throws ServletException, IOException {
           String userId = authenticationService.resolveUser(request);
           if (userId != null) {
               request.setAttribute("sprisk.userId", userId);
           }
           chain.doFilter(request, response);
       }
   }
   ```


4. **Challenge / block behaviour** – By default the starter throws exceptions when a challenge or block decision is reached. If you want JSON responses or other behaviour, register your own `ChallengeHandler` and/or `BlockHandler`. Whatever type your handler returns through `ChallengeResolution.returning(...)` must match the annotated method signature (for example `ResponseEntity<?>` in the demo app). If you want to return different types per endpoint, tailor the handler accordingly or keep the default exception-throwing strategy.

5. **Redis (optional)** – If your application exposes a `StringRedisTemplate` bean the starter automatically pings Redis. A successful ping enables the Redis-backed storage and logs `[Sprisk] RedisStorage activated`. If the ping fails you will see `[Sprisk] Redis unavailable, falling back to InMemoryStorage` and the in-memory store takes over transparently.

---

## 1. Architecture Layers

| Layer            | Module                     | Responsibility                                                                 |
|------------------|----------------------------|---------------------------------------------------------------------------------|
| Core             | `sprisk-engine-core`       | `RuleEngine`, `RiskResult`, `DecisionProfile`, low-level rule interfaces        |
| Spring Starter   | `sprisk-engine-starter`    | Auto-configuration, `@RiskCheck`, `RiskAspect`, hard-rule evaluation, handlers  |
| Example App      | `sprisk-engine-example-app`| Demonstrates handlers, listeners, composite resolver, and integration patterns  |

To enable Redis support simply expose `StringRedisTemplate` in your application context (see section 10).

---

## 2. Request Lifecycle

1. `RiskAspect` intercepts the `@RiskCheck` method and builds a `RiskInvocation`.
2. `RuleEngine` executes every registered `RiskRule`.
3. The combined score and triggered rules are stored in a `RiskResult`.
4. `HardRuleEvaluator` checks both the defaults and the YAML-defined hard rules.
5. `DecisionProfile` compares the score to the configured challenge and block thresholds.
6. If the decision is `CHALLENGE` or `BLOCK`, the relevant handler is invoked and returns a `ChallengeResolution`.
7. `ChallengeOutcomeListener` beans fire, letting you push metrics or logs downstream.
8. The current `HttpServletRequest` receives `spriskRuleFlags` and `spriskRuleFlagsString` attributes for debugging or telemetry.

---

## 3. Configuration Hierarchy

Priority order:  
(1) Rule class defaults → (2) `application.yaml` → (3) class-level `@RiskCheck` → (4) method-level `@RiskCheck` → (5) programmatic overrides.

### 3.1 YAML Example

```yaml
sprisk:
  enabled: true
  fail-closed: false
  challenge-threshold: 50
  block-threshold: 150
  timezone: Europe/Istanbul
  window-strategy: SLIDING

  policy:
    challenge-ttl: 2m
    temporary-block-ttl: 15m
    permanent-block-ttl: 7d
    escalation-threshold: 3
    permanent-block-enabled: true

  rules:
    ip-velocity:
      enabled: true
      window-seconds: 60
      max-per-window: 50
      risk-score: 30
    user-velocity:
      enabled: true
      window-seconds: 60
      max-per-window: 20
      risk-score: 40
    brute-force:
      enabled: false
      window-seconds: 300
      max-fail: 5
      risk-score: 60
    credential-stuffing:
      enabled: false
      window-seconds: 300
      max-distinct-user-count: 20
      risk-score: 70
    night-time:
      enabled: true
      start-hour: 1
      end-hour: 5
      risk-score: 20

  hard-rules:
    distributed-user-attack:
      match:
        ip-velocity: false
        user-velocity: true
      action: BLOCK
```

---

## 4. Challenge & Block Infrastructure

- `ChallengeResolution.proceed()/returning()/throwing()` drive how execution continues.
- `ChallengeOutcome` carries status, TTL, persistence, and metadata.
- `ChallengePolicyStrategy` determines which policy applies to the current request.
- Default handlers throw exceptions; supply your own implementations for REST-friendly responses or custom flows.
- When you return a value using `ChallengeResolution.returning(...)`, ensure the type matches what the intercepted method expects (string, DTO, `ResponseEntity`, etc.).

### Sample Challenge Handler

```java
@Component
public class ExampleChallengeHandler implements ChallengeHandler {

    private final ChallengeTelemetry telemetry;

    public ExampleChallengeHandler(ChallengeTelemetry telemetry) {
        this.telemetry = telemetry;
    }

    @Override
    public ChallengeResolution handleChallenge(ChallengeContext context) {
        telemetry.record(context);

        Duration ttl = context.policy() != null
                ? context.policy().challengeTtl()
                : Duration.ofMinutes(5);

        Map<String, Object> body = Map.of(
                "status", "CHALLENGE",
                "reason", context.reason(),
                "score", context.result().score(),
                "totalChallenges", telemetry.totalChallenges(),
                "retryAfterSeconds", ttl.toSeconds()
        );

        ChallengeOutcome outcome = ChallengeOutcome.challenge()
                .message(context.reason())
                .ttl(ttl)
                .metadata("hardRule", context.hardRuleHit() != null ? context.hardRuleHit().ruleName() : null)
                .build();

        return ChallengeResolution.returning(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body), outcome);
    }
}
```

### Sample Block Handler

```java
@Component
public class CustomBlockHandler implements BlockHandler {
    @Override
    public ChallengeResolution handleBlock(BlockContext context) {
        Map<String, Object> payload = Map.of(
                "status", "blocked",
                "reason", context.reason(),
                "score", context.result().score()
        );
        return ChallengeResolution.returning(ResponseEntity.status(HttpStatus.FORBIDDEN).body(payload));
    }
}
```

---

## 5. Hard Rule Details

Hard rules are defined under `sprisk.hard-rules`. The engine loads every rule irrespective of its identifier, so you can freely add names like `fraud-block` or `vip-allow`. Each rule declares a `match` map that references other rule codes and an `action` to execute (`BLOCK` or `CHALLENGE`). Keep the keys aligned with `RiskRule.code()` outputs.

Example:

```yaml
sprisk:
  hard-rules:
    vip-protection:
      match:
        brute-force: true
        user-velocity: true
      action: CHALLENGE
```

Rules are evaluated in YAML order; place more specific matches first.

---

## 6. Built-in Rules

| Rule                 | Description                                  | Default                                                         |
|----------------------|----------------------------------------------|-----------------------------------------------------------------|
| `IP_VELOCITY`        | Tracks per-IP request rate                   | `windowSeconds=60`, `maxPerWindow=50`, `riskScore=30`           |
| `USER_VELOCITY`      | Tracks request rate per user id              | `windowSeconds=60`, `maxPerWindow=20`, `riskScore=40`           |
| `BRUTE_FORCE`        | Counts failed attempts                       | `enabled=false`, `windowSeconds=300`, `maxFail=5`, `riskScore=60`|
| `CREDENTIAL_STUFFING`| Detects many user ids from the same IP       | `enabled=false`, `windowSeconds=300`, `maxDistinctUserCount=20`, `riskScore=70` |
| `NIGHT_TIME`         | Flags activity during night hours            | `enabled=true`, `startHour=2`, `endHour=6`, `riskScore=15`      |

Rules are customisable via YAML:

```yaml
sprisk:
  rules:
    ip-velocity:
      window-seconds: 30
      max-per-window: 15
      risk-score: 50
    brute-force:
      enabled: true
      max-fail: 3
      risk-score: 80
```

### Registering Custom Rules

The starter automatically gathers every `RiskRule` bean in the Spring context. To add your own heuristics, implement the interface and either annotate the class with `@Component` or expose it via a `@Bean`. Return a unique `code()` (used by hard rules and logs) and an `evaluate` score greater than zero when the rule should contribute risk.

```java
@Component
class SuspiciousIpRule implements RiskRule {

    private final Set<String> blockedIps;

    SuspiciousIpRule(DenyListService denyListService) {
        this.blockedIps = denyListService.currentEntries();
    }

    @Override
    public int evaluate(RiskContext ctx) {
        return blockedIps.contains(ctx.ip()) ? 80 : 0;
    }

    @Override
    public String code() {
        return "suspicious-ip";
    }
}
```

If you prefer Java configuration, declare the rule inside a configuration class:

```java
@Configuration
class CustomRuleConfiguration {

    @Bean
    RiskRule deviceVelocityRule(WindowManager windowManager) {
        return new RiskRule() {
            @Override
            public int evaluate(RiskContext ctx) {
                Map<String, Object> attributes = ctx.attributes();
                Integer recentDeviceCount = (Integer) attributes.getOrDefault("recentDeviceCount", 0);
                return recentDeviceCount > 3 ? 40 : 0;
            }

            @Override
            public String code() {
                return "device-velocity";
            }
        };
    }
}
```

As soon as the bean exists, `RuleEngine` logs it during startup and evaluates it alongside the built-ins. You can reference the returned `code()` in YAML hard rules or overrides exactly as you do with the default rules.

---

## 7. Redis Integration

When a `StringRedisTemplate` bean is present the starter issues a `PING` before enabling Redis storage:

- Successful ping → `[Sprisk] RedisStorage activated (Redis connection successful)`
- Failed ping → `[Sprisk] Redis unavailable, falling back to InMemoryStorage`

Redis configuration example:

```java
@Configuration
public class RedisClientConfiguration {
    @Bean
    public LettuceConnectionFactory redisConnectionFactory(RedisProperties props) {
        RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration(props.getHost(), props.getPort());
        cfg.setPassword(props.getPassword());
        return new LettuceConnectionFactory(cfg);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
```

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

For local development you can spin up Redis with `docker run --rm -p 6379:6379 redis:7-alpine`.

---

## 8. Troubleshooting

| Symptom                           | Likely Cause                                    | Resolution                                                           |
|-----------------------------------|--------------------------------------------------|----------------------------------------------------------------------|
| IP velocity triggers incorrectly  | Client IP not forwarded                         | Add `ForwardedHeaderFilter` or override `@RiskCheck(ip = ...)`       |
| User id resolves to `null`        | Resolver chain cannot locate an id              | Ensure headers/session/JWT provide a user id                         |
| Default hard rule blocks a user   | Same user accesses from multiple IPs            | Tweak or disable `sprisk.hard-rules.distributed-user-attack`         |
| Redis keys keep growing           | TTL/window values too long                      | Revisit `sprisk.policy.*` and per-rule window settings               |

Use the example app’s `RiskDebugLoggingFilter` to inspect request attributes during development.

---

## 9. Working with Maven Central

The published artefact coordinates are `io.github.sahinemirhan:sprisk-engine-starter`. Releases are tagged in Git with matching versions, and each release includes sources and javadoc jars. If you want to depend on an unreleased build, use `./gradlew publishToMavenLocal` and point your consuming project at `mavenLocal()` during development.

---

## 10. Contributing

1. Fork the repository and create feature branches from `main`.
2. Update documentation (`docs/`) whenever you add features or behaviour flags.
3. Add unit or integration tests for changes that affect challenge/block logic or rule outcomes.
4. Run `./gradlew build` before opening a pull request and attach the relevant output.
5. Follow the existing code style and avoid introducing unnecessary dependencies.

The project welcomes issues and discussions on GitHub. Bug reports with reproduction steps and proposed improvements are especially helpful.

---

For questions, open an issue in the repository or reach out to the maintainers. Happy building!
