package com.datlichkhambenh.backend.config;

import com.datlichkhambenh.backend.security.JwtTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtTokenFilter jwtTokenFilter;
    private final UserDetailsService userDetailsService;
    @org.springframework.beans.factory.annotation.Autowired
    private com.datlichkhambenh.backend.security.AuthRateLimitFilter rateLimitFilter;
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.core.env.Environment environment;

    public SecurityConfig(JwtTokenFilter jwtTokenFilter, UserDetailsService userDetailsService) {
        this.jwtTokenFilter = jwtTokenFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(org.springframework.security.config.Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 1. Công khai các API Auth & Actuator
                .requestMatchers("/api/v1/auth/change-password").authenticated()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/doctors/specialties", "/api/v1/clinic-info").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/actuator/prometheus").hasAnyAuthority("ADMIN", "METRICS")
                .requestMatchers("/actuator/**").hasAuthority("ADMIN")

                // 2. Yêu cầu xác thực JWT cho tất cả REST API ngoại trừ auth
                .requestMatchers("/api/v1/**").authenticated()

                // 3. Công khai toàn bộ Giao diện tĩnh (HTML/CSS/JS/Images) ở mọi thư mục (root, patient, doctor, admin...)
                .anyRequest().permitAll()
            )
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) -> response.sendError(401))
                .accessDeniedHandler((request, response, exception) -> response.sendError(403)))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new com.datlichkhambenh.backend.security.MetricsAuthenticationFilter(environment), JwtTokenFilter.class)
            .addFilterBefore(rateLimitFilter, JwtTokenFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
