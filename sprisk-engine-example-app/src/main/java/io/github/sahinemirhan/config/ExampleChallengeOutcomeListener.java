package io.github.sahinemirhan.config;

import io.github.sahinemirhan.starter.challenge.ChallengeContext;
import io.github.sahinemirhan.starter.challenge.ChallengeOutcome;
import io.github.sahinemirhan.starter.challenge.ChallengeOutcomeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Example listener that logs challenge and block outcomes for auditing.
 */
@Component
public class ExampleChallengeOutcomeListener implements ChallengeOutcomeListener {

    private static final Logger log = LoggerFactory.getLogger(ExampleChallengeOutcomeListener.class);

    @Override
    public void onChallengeOutcome(ChallengeOutcome outcome, ChallengeContext context) {
        log.info("[SampleApp] outcome={} decision={} ttl={} metadata={}",
                outcome.status(),
                context.decision(),
                outcome.ttl(),
                outcome.metadata());
    }
}

