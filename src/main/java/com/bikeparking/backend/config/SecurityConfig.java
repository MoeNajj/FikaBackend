package com.bikeparking.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Static token for Arduino/Unity devices (for testing)
    public static final String STATIC_DEVICE_TOKEN = "ARDUINO-UNITY-TEST-TOKEN-2024-12345";
    public static final String DEVICE_TOKEN_HEADER = "X-Device-Token";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/parking/**").permitAll() // Temporarily allow all - add JWT filter later
                .anyRequest().authenticated()
            );

        return http.build();
    }
}

// Note: RequestResponseLoggingFilter is automatically registered via @Component annotation
// It will log all requests and responses including auth endpoints

