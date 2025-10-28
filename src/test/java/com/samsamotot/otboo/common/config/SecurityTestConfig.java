package com.samsamotot.otboo.common.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRepository;

@TestConfiguration
@EnableWebSecurity
public class SecurityTestConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST,"/api/clothes/attribute-defs").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH,"/api/clothes/attribute-defs/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE,"/api/clothes/attribute-defs/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/sse").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/users/profiles/notification-weathers").authenticated()
                .requestMatchers("/api/notifications/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/direct-messages/rooms").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/feeds/{id}/hard").hasRole("ADMIN")
                .anyRequest().permitAll()
            )
            .build();
    }

    // ★ CsrfController가 요구하는 빈 제공
    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        var repo = org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse();
        repo.setCookieName("XSRF-TOKEN");
        repo.setHeaderName("X-XSRF-TOKEN");
        return repo;
    }
}