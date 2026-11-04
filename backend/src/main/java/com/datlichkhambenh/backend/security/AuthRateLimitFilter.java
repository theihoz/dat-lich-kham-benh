package com.datlichkhambenh.backend.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Fixed windows: Redis atomically shares limits across production replicas; demo/test stay local. */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {
 private final Environment environment;
 private final ObjectProvider<StringRedisTemplate> redis;
 private final Map<String,Window> windows=new ConcurrentHashMap<>();
 @Value("${app.auth.rate-limit.enabled:true}") private boolean enabled;
 @Value("${app.auth.rate-limit.login-limit:10}") private int loginLimit;
 private static final DefaultRedisScript<Long> INCREMENT=new DefaultRedisScript<>(
  "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('PEXPIRE',KEYS[1],ARGV[1]) end; return n",Long.class);
 public AuthRateLimitFilter(Environment environment,ObjectProvider<StringRedisTemplate> redis) { this.environment=environment;this.redis=redis; }
 protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
  String path=request.getRequestURI().substring(request.getContextPath().length());
  if(!enabled || !"POST".equals(request.getMethod()) || !limited(path)) { chain.doFilter(request,response);return; }
  int limit=path.endsWith("/login")?loginLimit:path.endsWith("/refresh")?60:5;
  long duration=path.endsWith("/login") || path.endsWith("/refresh")?60000:3600000;
  long window=System.currentTimeMillis()/duration;
  String key="clinic:auth:rate:"+AuthTokenSecrets.hash(path+":"+request.getRemoteAddr())+":"+window;
  long count;
  try {
   if(environment.acceptsProfiles(Profiles.of("prod"))) {
    StringRedisTemplate client=redis.getIfAvailable(); if(client==null) throw new IllegalStateException("Redis is required");
    Long value=client.execute(INCREMENT,List.of(key),String.valueOf(duration));
    if(value==null) throw new IllegalStateException("Missing rate-limit result");count=value;
   } else {
    if(windows.size()>10000) windows.entrySet().removeIf(e->e.getValue().expiresAt<System.currentTimeMillis());
    if(windows.size()>10000 && !windows.containsKey(key)) { reject(response,429,60);return; }
    Window bucket=windows.computeIfAbsent(key,k->new Window(System.currentTimeMillis()+duration));
    synchronized(bucket) { count=++bucket.count; }
   }
  } catch(RuntimeException ex) { reject(response,503,30);return; }
  if(count>limit) { reject(response,429,Math.max(1,(duration-System.currentTimeMillis()%duration+999)/1000));return; }
  chain.doFilter(request,response);
 }
 private boolean limited(String path) { return Set.of("/api/v1/auth/login","/api/v1/auth/register","/api/v1/auth/refresh",
  "/api/v1/auth/forgot-password","/api/v1/auth/reset-password","/api/v1/auth/resend-verification","/api/v1/auth/verify-email").contains(path); }
 private void reject(HttpServletResponse response,int status,long retry) throws IOException {
  response.setStatus(status);response.setHeader("Retry-After",String.valueOf(retry));response.setContentType("application/json;charset=UTF-8");
  response.getWriter().write("{\"success\":false,\"message\":\"Vui lòng thử lại sau.\"}");
 }
 private static final class Window { final long expiresAt;long count; Window(long expiry){expiresAt=expiry;} }
}
