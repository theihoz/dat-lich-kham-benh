package com.datlichkhambenh.backend.security;
import com.datlichkhambenh.backend.entity.User;
import com.datlichkhambenh.backend.exception.AppException;
import com.datlichkhambenh.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.*;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AuthPolicyTests {
 @Test void productionRejectsUnverifiedAccounts() {
  MockEnvironment env=new MockEnvironment();env.setActiveProfiles("prod");
  AuthSessionService service=new AuthSessionService(mock(AuthRefreshSessionRepository.class),mock(UserRepository.class),
   mock(DoctorRepository.class),mock(JwtTokenProvider.class),env);
  User user=User.builder().role("PATIENT").isActive(true).build();user.setEmailVerified(false);
  assertThrows(AppException.class,()->service.ensureCanAuthenticate(user));
  user.setEmailVerified(true);assertDoesNotThrow(()->service.ensureCanAuthenticate(user));
 }
 @Test void rateLimitReturns429AndRetryAfter() throws Exception {
  var filter=new AuthRateLimitFilter(new MockEnvironment(),mock(ObjectProvider.class));
  ReflectionTestUtils.setField(filter,"enabled",true);ReflectionTestUtils.setField(filter,"loginLimit",2);
  for(int i=0;i<3;i++) {
   var request=new MockHttpServletRequest("POST","/api/v1/auth/login");var response=new MockHttpServletResponse();
   filter.doFilter(request,response,new MockFilterChain());
   assertEquals(i<2?200:429,response.getStatus());
   if(i==2) assertTrue(Integer.parseInt(response.getHeader("Retry-After"))>0);
  }
 }
 @Test void productionRateLimitFailsClosedWithoutRedis() throws Exception {
  MockEnvironment env=new MockEnvironment();env.setActiveProfiles("prod");
  var filter=new AuthRateLimitFilter(env,mock(ObjectProvider.class));ReflectionTestUtils.setField(filter,"enabled",true);
  var response=new MockHttpServletResponse();filter.doFilter(new MockHttpServletRequest("POST","/api/v1/auth/login"),response,new MockFilterChain());
  assertEquals(503,response.getStatus());
 }
 @Test void productionRefreshCookieIsSecureAndHttpOnly() {
  MockEnvironment env=new MockEnvironment();env.setActiveProfiles("prod");
  var auth=mock(com.datlichkhambenh.backend.service.AuthService.class);
  when(auth.login(org.mockito.ArgumentMatchers.any())).thenReturn(com.datlichkhambenh.backend.dto.response.LoginResponseDto.builder()
   .accessToken("access").refreshToken("refresh").build());
  var controller=new com.datlichkhambenh.backend.controller.AuthController(auth,mock(AuthSessionService.class),mock(AuthActionTokenService.class),env);
  ReflectionTestUtils.setField(controller,"refreshTtl",604800000L);
  var response=new MockHttpServletResponse();
  controller.login(new com.datlichkhambenh.backend.dto.request.LoginRequestDto(),new MockHttpServletRequest(),response);
  String cookie=response.getHeader("Set-Cookie");assertTrue(cookie.contains("Secure"));assertTrue(cookie.contains("HttpOnly"));assertTrue(cookie.contains("SameSite=Strict"));
 }
}
