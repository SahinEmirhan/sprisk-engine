package com.spriskengine.config;

import com.spriskengine.starter.challenge.ChallengeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Demonstrates listening to challenge events for audit/logging.
 */
@Component
public class ChallengeEventLoggingListener {

    private static final Logger log = LoggerFactory.getLogger(ChallengeEventLoggingListener.class);

    @EventListener
    public void onChallenge(ChallengeEvent event) {
        log.info("[Example] Challenge event total={} reason={} score={}",
                event.totalChallenges(),
                event.context().reason(),
                event.context().result().score());
    }
}
