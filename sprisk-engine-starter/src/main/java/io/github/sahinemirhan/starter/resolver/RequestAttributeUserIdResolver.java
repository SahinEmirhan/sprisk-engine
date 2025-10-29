package io.github.sahinemirhan.starter.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;

public class RequestAttributeUserIdResolver implements RiskUserIdResolver {

    private static final String[] ATTRIBUTE_KEYS = {
            "sprisk.userId",
            "userId",
            "user-id"
    };

    @Override
    public String resolve(UserIdResolverContext context) {
        HttpServletRequest request = context.request();
        if (request == null) return null;
        for (String key : ATTRIBUTE_KEYS) {
            Object attribute = request.getAttribute(key);
            if (attribute instanceof String str && !str.isBlank()) {
                return str;
            }
        }
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 200;
    }
}

