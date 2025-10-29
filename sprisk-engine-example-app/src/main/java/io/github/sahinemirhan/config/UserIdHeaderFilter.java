package io.github.sahinemirhan.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class UserIdHeaderFilter implements Filter {

    private static final String HEADER_NAME = "X-User-Id";
    private static final String ATTRIBUTE_KEY = "sprisk.userId";
    private static final String SESSION_KEY = "demoUserId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest) {
            String header = httpRequest.getHeader(HEADER_NAME);
            if (header != null && !header.isBlank()) {
                httpRequest.setAttribute(ATTRIBUTE_KEY, header);
                HttpSession session = httpRequest.getSession(true);
                session.setAttribute(SESSION_KEY, header + "-session");
            }
        }
        chain.doFilter(request, response);
    }
}