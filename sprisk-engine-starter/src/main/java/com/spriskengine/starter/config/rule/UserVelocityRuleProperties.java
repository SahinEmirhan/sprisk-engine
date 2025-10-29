package com.spriskengine.starter.config.rule;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.HashSet;
import java.util.Set;

@ConfigurationProperties(prefix = "sprisk.rules.user-velocity")
public class UserVelocityRuleProperties {

    private boolean enabled = true;
    private long windowSeconds = 60;
    private long maxPerWindow = 20;
    private int riskScore = 35;
    // opsiyonel: dışlanacak aksiyonlar (CSV veya properties list)
    private Set<String> excludedActions = new HashSet<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public long getWindowSeconds() { return windowSeconds; }
    public void setWindowSeconds(long windowSeconds) { this.windowSeconds = windowSeconds; }

    public long getMaxPerWindow() { return maxPerWindow; }
    public void setMaxPerWindow(long maxPerWindow) { this.maxPerWindow = maxPerWindow; }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }

    public Set<String> getExcludedActions() { return excludedActions; }
    public void setExcludedActions(Set<String> excludedActions) { this.excludedActions = excludedActions; }
}

