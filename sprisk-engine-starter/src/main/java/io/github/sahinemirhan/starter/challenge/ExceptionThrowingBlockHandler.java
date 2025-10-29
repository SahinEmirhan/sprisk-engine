package io.github.sahinemirhan.starter.challenge;

import io.github.sahinemirhan.starter.exception.SpriskBlockedException;

/**
 * Default {@link BlockHandler} that translates block decisions into a {@link SpriskBlockedException}.
 */
public class ExceptionThrowingBlockHandler implements BlockHandler {

    @Override
    public ChallengeResolution handleBlock(BlockContext context) {
        ChallengeOutcome outcome = ChallengeOutcome.block()
                .message(context.reason())
                .ttl(context.policy().permanentBlockEnabled()
                        ? context.policy().permanentBlockTtl()
                        : context.policy().temporaryBlockTtl())
                .permanent(context.policy().permanentBlockEnabled())
                .metadata("score", context.result().score())
                .build();
        return ChallengeResolution.throwing(
                new SpriskBlockedException("Request blocked. Reason: " + context.reason()),
                outcome
        );
    }
}

