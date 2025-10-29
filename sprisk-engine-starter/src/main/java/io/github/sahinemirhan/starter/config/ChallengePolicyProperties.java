package io.github.sahinemirhan.starter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Challenge and block policy controls exposed to application configuration.
 */
@ConfigurationProperties(prefix = "sprisk.policy")
public class ChallengePolicyProperties {

    private Duration challengeTtl = Duration.ofMinutes(5);
    private Duration temporaryBlockTtl = Duration.ofMinutes(15);
    private Duration permanentBlockTtl = Duration.ofDays(365);
    private int escalationThreshold = 3;
    private boolean permanentBlockEnabled = true;

    public Duration getChallengeTtl() {
        return challengeTtl;
    }

    public void setChallengeTtl(Duration challengeTtl) {
        this.challengeTtl = challengeTtl;
    }

    public Duration getTemporaryBlockTtl() {
        return temporaryBlockTtl;
    }

    public void setTemporaryBlockTtl(Duration temporaryBlockTtl) {
        this.temporaryBlockTtl = temporaryBlockTtl;
    }

    public Duration getPermanentBlockTtl() {
        return permanentBlockTtl;
    }

    public void setPermanentBlockTtl(Duration permanentBlockTtl) {
        this.permanentBlockTtl = permanentBlockTtl;
    }

    public int getEscalationThreshold() {
        return escalationThreshold;
    }

    public void setEscalationThreshold(int escalationThreshold) {
        this.escalationThreshold = escalationThreshold;
    }

    public boolean isPermanentBlockEnabled() {
        return permanentBlockEnabled;
    }

    public void setPermanentBlockEnabled(boolean permanentBlockEnabled) {
        this.permanentBlockEnabled = permanentBlockEnabled;
    }
}

