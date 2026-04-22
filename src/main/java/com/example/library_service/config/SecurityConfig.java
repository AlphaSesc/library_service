package com.example.library_service.config;

import com.example.library_service.security.CustomLibraryUserDetailsService;
import com.example.library_service.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
// Central security configuration for authentication, authorization, and JWT-based request filtering
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomLibraryUserDetailsService customLibraryUserDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomLibraryUserDetailsService customLibraryUserDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customLibraryUserDetailsService = customLibraryUserDetailsService;
    }

    @Bean
    // Password encoder used to securely hash and verify user passwords
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    // Authentication provider that loads users from database and checks encoded passwords
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(customLibraryUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
        // Exposes AuthenticationManager so it can be used during login authentication
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    // Defines security rules for HTTP requests and integrates JWT authentication filter
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disables CSRF because this application uses stateless JWT authentication
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints accessible without authentication
                        .requestMatchers("/api/library/auth/**",
                                "/login",
                                "/dashboard",
                                "/books",
                                "/borrow-history",
                                "/my-borrowings",
                                "/admin/dashboard",
                                "/admin/add-book",
                                "/admin/books",
                                "/admin/current-loans",
                                "/admin/overdue",
                                "/admin/students")
                        .permitAll()

                        .requestMatchers("/api/library/register").permitAll()

                        // Admin-only endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // All other endpoints require authenticated user
                        .anyRequest().authenticated()
                )

                // Returns HTTP 401 when unauthenticated user tries to access protected endpoint
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                        ))

                // Disables server-side session storage because authentication is handled by JWT
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Uses custom authentication provider for login
                .authenticationProvider(authenticationProvider())

                // Runs JWT filter before Spring's default username/password authentication filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}