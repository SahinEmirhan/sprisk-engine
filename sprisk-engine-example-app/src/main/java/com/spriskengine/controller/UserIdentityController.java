package com.spriskengine.controller;

import com.spriskengine.context.ExampleUserContext;
import com.spriskengine.config.ExampleUserContextFilter;
import com.spriskengine.starter.annotation.RiskCheck;
import com.spriskengine.starter.resolver.RiskUserIdResolver;
import com.spriskengine.starter.resolver.UserIdResolverContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/demo/user")
@RiskCheck(action = "USER_ID_DEMO")
public class UserIdentityController {

    private final RiskUserIdResolver userIdResolver;

    public UserIdentityController(RiskUserIdResolver userIdResolver) {
        this.userIdResolver = userIdResolver;
    }

    @GetMapping("/resolved")
    public Map<String, Object> resolvedUser(HttpServletRequest request) {
        String resolved = userIdResolver.resolve(new UserIdResolverContext(null, request));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("resolvedUserId", resolved);
        result.put("requestAttribute", request.getAttribute("sprisk.userId"));
        result.put("sessionAttribute", readSessionUser(request.getSession(false)));
        result.put("contextUserId", ExampleUserContext.get());
        result.put("jwtPayloadExample", extractDemoJwtPayload(request));
        return result;
    }

    private Object readSessionUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        return session.getAttribute(ExampleUserContextFilter.SESSION_ATTRIBUTE);
    }

    private String extractDemoJwtPayload(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("Authorization"))
                .filter(value -> value.startsWith("Bearer demo."))
                .map(value -> value.substring("Bearer demo.".length()))
                .map(token -> token.split("\\."))
                .filter(parts -> parts.length > 0)
                .map(parts -> parts[0])
                .orElse(null);
    }
}

