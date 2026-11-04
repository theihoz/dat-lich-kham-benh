package com.datlichkhambenh.backend.controller;

import com.datlichkhambenh.backend.dto.request.*;
import com.datlichkhambenh.backend.dto.response.*;
import com.datlichkhambenh.backend.exception.AppException;
import com.datlichkhambenh.backend.security.*;
import com.datlichkhambenh.backend.service.AuthService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.time.Duration;

@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
 private final AuthService auth;
 private final AuthSessionService sessions;
 private final AuthActionTokenService actions;
 private final Environment environment;
 @Value("${app.public-url:http://localhost:18080}") private String publicUrl;
 @Value("${app.jwt.refresh-expiration-ms:604800000}") private long refreshTtl;
 public AuthController(AuthService auth, AuthSessionService sessions, AuthActionTokenService actions,Environment environment) {
  this.auth=auth;this.sessions=sessions;this.actions=actions;this.environment=environment;
 }
 @PostMapping("/register") public ResponseEntity<ApiResponseDto<UserDto>> register(@Valid @RequestBody RegisterRequestDto request) {
  return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.success(auth.register(request),"Đăng ký thành công. Vui lòng kiểm tra email để xác minh tài khoản."));
 }
 @PostMapping("/login") public ResponseEntity<ApiResponseDto<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request,HttpServletRequest http,HttpServletResponse response) {
  LoginResponseDto login=auth.login(request); return respond(login,http,response);
 }
 @PostMapping("/refresh") public ResponseEntity<ApiResponseDto<LoginResponseDto>> refresh(@RequestBody(required=false) RefreshTokenRequestDto body,HttpServletRequest http,HttpServletResponse response) {
  try { return respond(auth.refreshToken(new RefreshTokenRequestDto(refreshValue(body,http))),http,response); }
  catch(AppException ex) { cookie(response,"",http,Duration.ZERO);throw ex; }
 }
 @PostMapping("/logout") public ApiResponseDto<Void> logout(@RequestBody(required=false) RefreshTokenRequestDto body,HttpServletRequest http,HttpServletResponse response) {
  sessions.logout(refreshValue(body,http)); cookie(response,"",http,Duration.ZERO);return ApiResponseDto.success(null,"Đã đăng xuất");
 }
 @PostMapping("/change-password") public ApiResponseDto<Void> changePassword(@AuthenticationPrincipal UserDetails user,@Valid @RequestBody ChangePasswordDto request,HttpServletRequest http,HttpServletResponse response) {
  auth.changePassword(user.getUsername(),request);cookie(response,"",http,Duration.ZERO);return ApiResponseDto.success(null,"Đã đổi mật khẩu. Vui lòng đăng nhập lại.");
 }
 @PostMapping("/forgot-password") public ApiResponseDto<Void> forgot(@Valid @RequestBody AccountEmailRequest request) {
  actions.request(request.getEmail(),AuthActionTokenService.RESET);return generic();
 }
 @PostMapping("/resend-verification") public ApiResponseDto<Void> resend(@Valid @RequestBody AccountEmailRequest request) {
  actions.request(request.getEmail(),AuthActionTokenService.VERIFY);return generic();
 }
 @PostMapping("/reset-password") public ApiResponseDto<Void> reset(@Valid @RequestBody ResetPasswordRequest request) {
  actions.reset(request.getToken(),request.getPassword());return ApiResponseDto.success(null,"Mật khẩu đã đổi. Vui lòng đăng nhập lại.");
 }
 @PostMapping("/verify-email") public ApiResponseDto<Void> verify(@Valid @RequestBody AccountTokenRequest request) {
  actions.verify(request.getToken());return ApiResponseDto.success(null,"Email đã được xác minh.");
 }
 private ApiResponseDto<Void> generic() { return ApiResponseDto.success(null,"Nếu tài khoản phù hợp tồn tại, chúng tôi sẽ gửi liên kết qua email."); }
 private ResponseEntity<ApiResponseDto<LoginResponseDto>> respond(LoginResponseDto login,HttpServletRequest request,HttpServletResponse response) {
  if(!apiClient(request)) { cookie(response,login.getRefreshToken(),request,Duration.ofMillis(refreshTtl));login.setRefreshToken(null); }
  response.setHeader("Cache-Control","no-store");return ResponseEntity.ok(ApiResponseDto.success(login,"Thành công"));
 }
 private boolean apiClient(HttpServletRequest request) { return "api".equals(request.getHeader("X-Auth-Client")); }
 private String refreshValue(RefreshTokenRequestDto body,HttpServletRequest request) {
  if(apiClient(request)) {
   // API mode never consumes ambient cookies. Cross-origin browser calls cannot bypass Origin checks.
   if(request.getHeader("Origin")!=null) checkOrigin(request);
   if(body==null || body.getRefreshToken()==null) throw new AppException("Thiếu refresh token",HttpStatus.UNAUTHORIZED);
   return body.getRefreshToken();
  }
  checkOrigin(request);
  if(!"clinic".equals(request.getHeader("X-CSRF-Token"))) throw new AppException("CSRF header không hợp lệ",HttpStatus.FORBIDDEN);
  if(request.getCookies()!=null) for(Cookie cookie:request.getCookies()) if("clinic_refresh".equals(cookie.getName())) return cookie.getValue();
  throw new AppException("Phiên đăng nhập đã hết hạn",HttpStatus.UNAUTHORIZED);
 }
 private void checkOrigin(HttpServletRequest request) {
  String expected=URI.create(publicUrl).getScheme()+"://"+URI.create(publicUrl).getRawAuthority();
  if(!expected.equals(request.getHeader("Origin"))) throw new AppException("Origin không được phép",HttpStatus.FORBIDDEN);
 }
 private void cookie(HttpServletResponse response,String token,HttpServletRequest request,Duration age) {
  boolean secure=environment.acceptsProfiles(Profiles.of("prod")) || request.isSecure();
  response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from("clinic_refresh",token).httpOnly(true).secure(secure)
   .sameSite("Strict").path("/api/v1/auth").maxAge(age).build().toString());
 }
}
