package com.vanilo.psych.agent.config;

import com.vanilo.psych.agent.security.CustomUserDetailsService;
import com.vanilo.psych.agent.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    public SecurityConfig(CustomUserDetailsService customUserDetailsService, JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.customUserDetailsService = customUserDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .formLogin(formLogin -> formLogin.disable())
                .userDetailsService(customUserDetailsService)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth->auth
                        .requestMatchers("/auth/**", "/test/ping", "/help/**", "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/reports/my", "/reports/dashboard", "/chat", "/chat/analyze", "/message", "/agent/chat", "/conversations/**", "/role-cards/**", "/tools/**", "/feedback", "/feedback/my").authenticated()
                        .requestMatchers("/admin/**", "/feedback/**").hasRole("ADMIN")
                        .requestMatchers("/profile","/profile/**").hasRole("ADMIN")
                        .requestMatchers("/knowledge/**").hasRole("ADMIN")
                        .requestMatchers("/test/**").hasRole("ADMIN")
                        .requestMatchers("/reports/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(401);
                            response.getWriter().write("{\"success\":false,\"message\":\"未认证\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(403);
                            response.getWriter().write("{\"success\":false,\"message\":\"无权限\"}");
                        })
                );
        return http.build();
    }
}
