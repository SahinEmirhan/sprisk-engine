package io.github.sahinemirhan.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RiskDebugLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RiskDebugLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            Object flags = request.getAttribute("spriskRuleFlagsString");
            Object enabledOverrides = request.getAttribute("spriskRuleEnabledOverrides");
            Object propertyOverrides = request.getAttribute("spriskRulePropertyOverrides");

            if (flags != null) {
                log.info("spriskRuleFlagsString={} path={} status={}", flags, request.getRequestURI(), response.getStatus());
            }
            if (enabledOverrides != null) {
                log.debug("spriskRuleEnabledOverrides={}", enabledOverrides);
            }
            if (propertyOverrides != null) {
                log.debug("spriskRulePropertyOverrides={}", propertyOverrides);
            }
        }
    }
}

