package com.TalentForge.talentforge.config;

import com.TalentForge.talentforge.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error", "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login/roles").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/users/me").authenticated()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/ws/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/options").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/jobs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/jobs/**").hasAnyRole("RECRUITER", "CANDIDATE", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/jobs/**").hasAnyRole("RECRUITER", "CANDIDATE", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/jobs/**").hasAnyRole("RECRUITER", "CANDIDATE", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/applications/*/rescore").hasAnyRole("RECRUITER", "ADMIN")
                        .requestMatchers("/api/v1/payments/**").hasAnyRole("RECRUITER", "CANDIDATE", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/subscriptions/**").hasAnyRole("ADMIN", "RECRUITER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/interviews/**").hasAnyRole("RECRUITER", "ADMIN")
                        .requestMatchers("/api/v1/integrations/**").hasAnyRole("RECRUITER", "ADMIN")
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
