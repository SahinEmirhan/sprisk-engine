package io.github.sahinemirhan.starter.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Composable {@link RiskUserIdResolver} implementation that tries a sequence of extractor functions.
 * Applications can configure a single bean and decide in which order potential user id sources should be evaluated.
 */
public final class CompositeUserIdResolver implements RiskUserIdResolver {

    @FunctionalInterface
    public interface Extractor {
        String extract(UserIdResolverContext context);
    }

    private final List<Extractor> extractors;
    private final int order;

    private CompositeUserIdResolver(List<Extractor> extractors, int order) {
        this.extractors = List.copyOf(extractors);
        this.order = order;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String resolve(UserIdResolverContext context) {
        for (Extractor extractor : extractors) {
            String value = extractor.extract(context);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    @Override
    public int getOrder() {
        return order;
    }

    public static Extractor fromHeaders(String... headerNames) {
        List<String> headers = normalize(headerNames);
        return context -> {
            HttpServletRequest request = context.request();
            if (request == null) {
                return null;
            }
            for (String header : headers) {
                String value = request.getHeader(header);
                if (StringUtils.hasText(value)) {
                    return value;
                }
            }
            return null;
        };
    }

    public static Extractor fromRequestAttributes(String... attributeNames) {
        List<String> attributes = normalize(attributeNames);
        return context -> {
            HttpServletRequest request = context.request();
            if (request == null) {
                return null;
            }
            for (String attribute : attributes) {
                Object value = request.getAttribute(attribute);
                if (value instanceof String str && StringUtils.hasText(str)) {
                    return str;
                }
            }
            return null;
        };
    }

    public static Extractor fromPrincipal(Function<Principal, String> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return context -> {
            HttpServletRequest request = context.request();
            if (request == null) {
                return null;
            }
            Principal principal = request.getUserPrincipal();
            if (principal == null) {
                return null;
            }
            String value = mapper.apply(principal);
            return StringUtils.hasText(value) ? value : null;
        };
    }

    private static List<String> normalize(String... values) {
        if (values == null || values.length == 0) {
            return List.of();
        }
        return Arrays.stream(values)
                .filter(StringUtils::hasText)
                .toList();
    }

    public static final class Builder {

        private final List<Extractor> extractors = new ArrayList<>();
        private int order = Ordered.LOWEST_PRECEDENCE;

        private Builder() {
        }

        public Builder attempt(Extractor extractor) {
            if (extractor != null) {
                extractors.add(extractor);
            }
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public CompositeUserIdResolver build() {
            if (extractors.isEmpty()) {
                throw new IllegalStateException("At least one extractor must be configured");
            }
            return new CompositeUserIdResolver(extractors, order);
        }
    }
}
