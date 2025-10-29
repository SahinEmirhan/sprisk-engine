package io.github.sahinemirhan.starter.resolver;

import org.springframework.core.Ordered;

public interface RiskUserIdResolver extends Ordered {

    String resolve(UserIdResolverContext context);

    @Override
    default int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}

