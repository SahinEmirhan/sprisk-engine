package com.spriskengine.starter.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;

public record UserIdResolverContext(
        ProceedingJoinPoint joinPoint,
        HttpServletRequest request
) {
}

