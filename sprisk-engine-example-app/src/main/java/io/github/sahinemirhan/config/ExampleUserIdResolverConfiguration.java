package io.github.sahinemirhan.config;

import io.github.sahinemirhan.context.ExampleUserContext;
import io.github.sahinemirhan.starter.resolver.CompositeUserIdResolver;
import io.github.sahinemirhan.starter.resolver.RiskUserIdResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Example configuration that demonstrates how a single composite resolver can try
 * multiple user id sources in a defined order.
 */
@Configuration
public class ExampleUserIdResolverConfiguration {

    @Bean
    public RiskUserIdResolver exampleUserIdResolver() {
        return CompositeUserIdResolver.builder()
                .attempt(context -> ExampleUserContext.get())
                .attempt(CompositeUserIdResolver.fromRequestAttributes("sprisk.userId", "userId"))
                .attempt(context -> resolveSessionUser(context.request()))
                .attempt(CompositeUserIdResolver.fromHeaders("X-User-Id"))
                .attempt(context -> decodeDemoJwt(context.request()))
                .build();
    }

    private String resolveSessionUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(ExampleUserContextFilter.SESSION_ATTRIBUTE);
        if (value instanceof String str && StringUtils.hasText(str)) {
            return str;
        }
        return null;
    }

    private String decodeDemoJwt(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            return null;
        }
        String token = header.substring(7).trim();
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            return null;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        // Gerçekte: jwtDecoder.decode(token).getClaim("sub") vb.
    }
}

