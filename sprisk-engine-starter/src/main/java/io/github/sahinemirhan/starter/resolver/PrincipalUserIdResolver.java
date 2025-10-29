package io.github.sahinemirhan.starter.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;

public class PrincipalUserIdResolver implements RiskUserIdResolver {

    @Override
    public String resolve(UserIdResolverContext context) {
        HttpServletRequest request = context.request();
        if (request == null || request.getUserPrincipal() == null) {
            return null;
        }
        String name = request.getUserPrincipal().getName();
        return (name == null || name.isBlank()) ? null : name;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 100;
    }
}
