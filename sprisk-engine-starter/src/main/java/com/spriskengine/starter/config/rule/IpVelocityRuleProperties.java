package com.spriskengine.starter.config.rule;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sprisk.rules.ip-velocity")
public class IpVelocityRuleProperties {

    private boolean enabled = true;
    private long windowSeconds = 60;
    private long maxPerWindow = 25;
    private int riskScore = 50;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public long getWindowSeconds() { return windowSeconds; }
    public void setWindowSeconds(long windowSeconds) { this.windowSeconds = windowSeconds; }

    public long getMaxPerWindow() { return maxPerWindow; }
    public void setMaxPerWindow(long maxPerWindow) { this.maxPerWindow = maxPerWindow; }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
}
