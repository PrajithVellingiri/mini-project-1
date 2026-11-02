package com.transport.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthFilter,
            CustomUserDetailsService userDetailsService,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint,
            RestAccessDeniedHandler restAccessDeniedHandler,
            CorsConfigurationSource corsConfigurationSource
    ) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
        this.restAccessDeniedHandler = restAccessDeniedHandler;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/health", "/api/health/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // User profile check
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()

                        // Admin User Management
                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        // Destructive simulation control (Admin only)
                        .requestMatchers(HttpMethod.POST, "/api/simulation/reset").hasRole("ADMIN")

                        // Operational control: dispatch, demand injection, traffic adjustments, analytics, breakdowns
                        .requestMatchers(HttpMethod.POST, "/api/simulation/run").hasAnyRole("ADMIN", "TRANSPORT_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/simulation/complete-trips").hasAnyRole("ADMIN", "TRANSPORT_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/simulation/demand").hasAnyRole("ADMIN", "TRANSPORT_MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/simulation/analytics").hasAnyRole("ADMIN", "TRANSPORT_MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/api/simulation/roads/**").hasAnyRole("ADMIN", "TRANSPORT_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/simulation/buses/{id}/breakdown").hasAnyRole("ADMIN", "TRANSPORT_MANAGER")

                        // Driver permitted operational actions
                        .requestMatchers(HttpMethod.POST, "/api/simulation/trips/{tripId}/complete").hasAnyRole("ADMIN", "TRANSPORT_MANAGER", "DRIVER")
                        .requestMatchers(HttpMethod.PATCH, "/api/simulation/buses/{id}/status").hasAnyRole("ADMIN", "TRANSPORT_MANAGER", "DRIVER")
                        .requestMatchers(HttpMethod.POST, "/api/simulation/buses/{id}/restore").hasAnyRole("ADMIN", "TRANSPORT_MANAGER", "DRIVER")

                        // Route search and read-only state available to all authenticated roles
                        .requestMatchers(HttpMethod.GET, "/api/simulation/routes").hasAnyRole("ADMIN", "TRANSPORT_MANAGER", "DRIVER", "PASSENGER")
                        .requestMatchers(HttpMethod.GET, "/api/simulation/state").hasAnyRole("ADMIN", "TRANSPORT_MANAGER", "DRIVER", "PASSENGER")

                        // Fallback: all other /api/** endpoints require authentication
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
