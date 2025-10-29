package com.spriskengine.starter.config;

import com.spriskengine.core.model.Decision;
import com.spriskengine.core.window.WindowStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "sprisk")
public class SpriskProperties {

    private boolean enabled = true;
    private boolean failClosed = false;
    private int challengeThreshold = 50;
    private int blockThreshold = 80;
    private String timezone = "Europe/Istanbul";
    private WindowStrategy windowStrategy = WindowStrategy.SLIDING;
    private Map<String, HardRuleConfig> hardRules = new LinkedHashMap<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isFailClosed() { return failClosed; }
    public void setFailClosed(boolean failClosed) { this.failClosed = failClosed; }

    public int getChallengeThreshold() { return challengeThreshold; }
    public void setChallengeThreshold(int challengeThreshold) { this.challengeThreshold = challengeThreshold; }

    public int getBlockThreshold() { return blockThreshold; }
    public void setBlockThreshold(int blockThreshold) { this.blockThreshold = blockThreshold; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public WindowStrategy getWindowStrategy() { return windowStrategy; }
    public void setWindowStrategy(WindowStrategy windowStrategy) { this.windowStrategy = windowStrategy; }

    public Map<String, HardRuleConfig> getHardRules() {
        return hardRules;
    }

    public void setHardRules(Map<String, HardRuleConfig> hardRules) {
        this.hardRules = hardRules != null ? new LinkedHashMap<>(hardRules) : new LinkedHashMap<>();
    }

    public static class HardRuleConfig {
        private Map<String, Boolean> match = new LinkedHashMap<>();
        private Decision action = Decision.BLOCK;

        public Map<String, Boolean> getMatch() {
            return match;
        }

        public void setMatch(Map<String, Boolean> match) {
            this.match = match != null ? new LinkedHashMap<>(match) : new LinkedHashMap<>();
        }

        public Decision getAction() {
            return action;
        }

        public void setAction(Decision action) {
            this.action = action != null ? action : Decision.BLOCK;
        }
    }
}

