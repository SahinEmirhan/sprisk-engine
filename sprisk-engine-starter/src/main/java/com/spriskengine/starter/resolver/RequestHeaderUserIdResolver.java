package com.spriskengine.starter.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;

public class RequestHeaderUserIdResolver implements RiskUserIdResolver {

    private static final String[] HEADER_KEYS = {
            "X-User-Id",
            "X-USER-ID",
            "X_USER_ID"
    };

    @Override
    public String resolve(UserIdResolverContext context) {
        HttpServletRequest request = context.request();
        if (request == null) return null;
        for (String key : HEADER_KEYS) {
            String value = request.getHeader(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 150;
    }
}
