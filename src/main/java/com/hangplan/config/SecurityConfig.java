package com.hangplan.config;

import com.hangplan.security.JwtAuthFilter;
import com.hangplan.security.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;
    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String googleClientSecret;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        boolean googleOAuthEnabled = hasValue(googleClientId) && hasValue(googleClientSecret)
                && !"change-me".equals(googleClientId)
                && !"change-me".equals(googleClientSecret);

        http
                .cors(c -> {})
                .csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(
                                "/auth/**",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/error"
                        ).permitAll()
                        .anyRequest().authenticated()
                );

        if (googleOAuthEnabled) {
            http.oauth2Login(o -> o.successHandler(oAuth2SuccessHandler));
        }

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }
}
