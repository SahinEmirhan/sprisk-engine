package io.github.sahinemirhan.resolver;

import io.github.sahinemirhan.starter.resolver.CompositeUserIdResolver;
import io.github.sahinemirhan.starter.resolver.UserIdResolverContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class CompositeUserIdResolverTest {

    @Test
    void resolvesUsingFirstNonEmptyExtractor() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("X-User-Id")).thenReturn("alice");

        CompositeUserIdResolver resolver = CompositeUserIdResolver.builder()
                .attempt(CompositeUserIdResolver.fromRequestAttributes("sprisk.userId"))
                .attempt(CompositeUserIdResolver.fromHeaders("X-User-Id"))
                .build();

        UserIdResolverContext context = new UserIdResolverContext(null, request);

        assertEquals("alice", resolver.resolve(context));
    }

    @Test
    void returnsNullWhenAllExtractorsFail() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        CompositeUserIdResolver resolver = CompositeUserIdResolver.builder()
                .attempt(CompositeUserIdResolver.fromHeaders("X-User-Id"))
                .attempt(ctx -> null)
                .build();

        UserIdResolverContext context = new UserIdResolverContext(null, request);

        assertNull(resolver.resolve(context));
    }

    @Test
    void builderRequiresAtLeastOneExtractor() {
        CompositeUserIdResolver.Builder builder = CompositeUserIdResolver.builder();
        assertThrows(IllegalStateException.class, builder::build);
    }
}

