package io.github.sahinemirhan.config;

import io.github.sahinemirhan.starter.challenge.BlockContext;
import io.github.sahinemirhan.starter.challenge.BlockHandler;
import io.github.sahinemirhan.starter.challenge.ChallengeOutcome;
import io.github.sahinemirhan.starter.challenge.ChallengeResolution;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Sample block handler showcasing how to return a structured HTTP 403 response instead of throwing.
 */
@Component
public class ExampleBlockHandler implements BlockHandler {

    @Override
    public ChallengeResolution handleBlock(BlockContext context) {
        Map<String, Object> body = Map.of(
                "status", "BLOCKED",
                "reason", context.reason(),
                "score", context.result().score(),
                "permanent", context.policy().permanentBlockEnabled(),
                "ttlSeconds", context.policy().ttlFor(ChallengeOutcome.Status.BLOCK).toSeconds()
        );
        ResponseEntity<Map<String, Object>> response = ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        ChallengeOutcome outcome = ChallengeOutcome.block()
                .message(context.reason())
                .ttl(context.policy().ttlFor(ChallengeOutcome.Status.BLOCK))
                .permanent(context.policy().permanentBlockEnabled())
                .metadata(body)
                .build();
        return ChallengeResolution.returning(response, outcome);
    }
}

