package io.github.sahinemirhan.config;

import io.github.sahinemirhan.context.ExampleUserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Demo filter that illustrates how an application might project user identifiers into different carriers.
 * <p>
 * Bu filtre isteğe gelen özel header'ları okuyarak userId bilgisini request attribute'una, HTTP session'a
 * veya thread-local context'e taşır. Uygulamanızda mevcut authentication katmanınızda hangi mekanizma varsa,
 * benzer şekilde userId bilgisini Sprisk'in okuyabileceği bir kaynağa yerleştirmeniz yeterlidir.
 */
@Component
public class ExampleUserContextFilter extends OncePerRequestFilter {

    public static final String SESSION_ATTRIBUTE = "example.session.userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            propagateRequestAttribute(request);
            propagateSessionAttribute(request);
            propagateThreadContext(request);
            filterChain.doFilter(request, response);
        } finally {
            ExampleUserContext.clear();
        }
    }

    private void propagateRequestAttribute(HttpServletRequest request) {
        String headerValue = request.getHeader("X-Demo-Request-User");
        if (StringUtils.hasText(headerValue)) {
            request.setAttribute("sprisk.userId", headerValue);
        }
    }

    private void propagateSessionAttribute(HttpServletRequest request) {
        String sessionUser = request.getHeader("X-Demo-Session-User");
        if (!StringUtils.hasText(sessionUser)) {
            return;
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_ATTRIBUTE, sessionUser);
    }

    private void propagateThreadContext(HttpServletRequest request) {
        String contextUser = request.getHeader("X-Demo-Context-User");
        if (StringUtils.hasText(contextUser)) {
            ExampleUserContext.set(contextUser);
        }
    }
}

