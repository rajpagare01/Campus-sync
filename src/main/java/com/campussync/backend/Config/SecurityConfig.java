package com.campussync.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/api/v1/files/**").authenticated()
                        .requestMatchers("/files/**").authenticated()
                        .requestMatchers("/ws/notifications/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/webhook").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/events").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events").permitAll()
                        .requestMatchers(HttpMethod.GET, "/events/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/events/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/follow/stats/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/follow/stats/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users/*/profile").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/*/profile").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users/*/stats").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/*/stats").permitAll()
                        .requestMatchers(HttpMethod.POST, "/events").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/events").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/events/*").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/events/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/events/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/events/*").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/events/*/status").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/events/*/status").authenticated()
                        .requestMatchers(HttpMethod.GET, "/events/*/analytics").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/*/analytics").authenticated()
                        .requestMatchers("/dashboard/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/dashboard/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/registrations/**").authenticated()
                        .requestMatchers("/api/v1/registrations/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
