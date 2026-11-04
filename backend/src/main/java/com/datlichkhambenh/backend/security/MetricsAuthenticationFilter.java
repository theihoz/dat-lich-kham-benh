package com.datlichkhambenh.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/** Installed only in the Security chain; the credential is scoped to one scrape endpoint. */
public class MetricsAuthenticationFilter extends OncePerRequestFilter {
    private final Environment environment;
    public MetricsAuthenticationFilter(Environment environment) { this.environment=environment; }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !environment.acceptsProfiles(Profiles.of("prod")) || !"/actuator/prometheus".equals(request.getRequestURI());
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        String secret=environment.getProperty("app.metrics-token","");
        String header=request.getHeader("Authorization");
        if(secret.length()>=32 && header!=null && MessageDigest.isEqual(("Bearer "+secret).getBytes(StandardCharsets.UTF_8),header.getBytes(StandardCharsets.UTF_8))) {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("metrics-scraper",null,List.of(new SimpleGrantedAuthority("METRICS"))));
        }
        chain.doFilter(request,response);
    }
}
