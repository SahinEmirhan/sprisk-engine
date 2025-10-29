package com.spriskengine.starter.config.rule;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sprisk.rules.bruteforce")
public class BruteForceRuleProperties {

    private boolean enabled = true;
    private long windowSeconds = 300;
    private long maxFail = 8;
    private int riskScore = 50;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public long getWindowSeconds() { return windowSeconds; }
    public void setWindowSeconds(long windowSeconds) { this.windowSeconds = windowSeconds; }

    public long getMaxFail() { return maxFail; }
    public void setMaxFail(long maxFail) { this.maxFail = maxFail; }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
}
