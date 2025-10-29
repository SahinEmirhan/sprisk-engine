package io.github.sahinemirhan.starter.config.rule;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sprisk.rules.credential-stuffing")
public class CredentialStuffingRuleProperties {

    private boolean enabled = true;
    private long windowSeconds = 300;         // sliding window in seconds
    private long maxDistinctUserCount = 5;   // threshold
    private int riskScore = 70;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public long getWindowSeconds() { return windowSeconds; }
    public void setWindowSeconds(long windowSeconds) { this.windowSeconds = windowSeconds; }

    public long getMaxDistinctUserCount() { return maxDistinctUserCount; }
    public void setMaxDistinctUserCount(long maxDistinctUserCount) { this.maxDistinctUserCount = maxDistinctUserCount; }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
}

