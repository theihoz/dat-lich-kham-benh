package com.datlichkhambenh.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;

@Component("clinicRequestContextFilter")
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class RequestContextFilter extends OncePerRequestFilter {
    private final Environment environment;
    public RequestContextFilter(Environment environment) { this.environment=environment; }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId",requestId); response.setHeader("X-Request-ID",requestId);
        response.setHeader("Referrer-Policy","no-referrer");
        response.setHeader("Permissions-Policy","camera=(), microphone=(), geolocation=()");
        boolean prod=environment.acceptsProfiles(Profiles.of("prod"));
        String path=request.getRequestURI();
        try {
            if(prod) {
                response.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; font-src 'self'; img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'; upgrade-insecure-requests");
                if(path.equals("/patterns.html") || path.startsWith("/api/v1/admin/demo-notifications")) { response.setStatus(404); return; }
            }
            if(path.startsWith("/api/")) response.setHeader("Cache-Control","no-store");
            chain.doFilter(request,response);
        } finally { MDC.remove("requestId"); }
    }
}
