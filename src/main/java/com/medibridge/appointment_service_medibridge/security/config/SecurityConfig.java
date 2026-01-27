package com.medibridge.appointment_service_medibridge.security.config;

import com.medibridge.appointment_service_medibridge.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Security Configuration for Appointment Service
 *
 * - JWT-based authentication
 * - Stateless sessions
 * - WebSocket + SockJS allowed without auth for handshake
 * - STOMP CONNECT authenticated via WebSocketAuthInterceptor
 * - Returns 401 JSON instead of triggering browser login popup
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for stateless REST API
                .csrf(AbstractHttpConfigurer::disable)

                // Enable CORS with proper configuration
//                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .cors(AbstractHttpConfigurer::disable)

                // Stateless sessions (JWT-based)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configure exception handling to return 401 instead of redirect
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

                // Disable HTTP Basic authentication popup
                .httpBasic(AbstractHttpConfigurer::disable)

                // Disable form login
                .formLogin(AbstractHttpConfigurer::disable)

                // Authorization rules
//                .authorizeHttpRequests(auth -> auth
//                        // ============================================
//                        // WEBSOCKET ENDPOINTS (SockJS + STOMP)
//                        // ============================================
//                        // All WebSocket requests bypass HTTP security
//                        // STOMP CONNECT is authenticated by WebSocketAuthInterceptor
//                        // SockJS handshake (/ws/info, /ws/xhr, etc) requires no HTTP auth
//                        .requestMatchers(HttpMethod.GET, "/ws/**").permitAll()      // SockJS info handshake
//                        .requestMatchers(HttpMethod.POST, "/ws/**").permitAll()     // SockJS transports (XHR, iframe)
//                        .requestMatchers(HttpMethod.OPTIONS, "/ws/**").permitAll()  // CORS preflight
//
//                        // ============================================
//                        // CORS PREFLIGHT (OPTIONS)
//                        // ============================================
//                        // Allow CORS preflight for all paths
//                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
//
//                        // ============================================
//                        // HEALTH & ACTUATOR
//                        // ============================================
//                        // Allow health checks
//                        .requestMatchers("/actuator/**").permitAll()
//                        .requestMatchers("/health").permitAll()
//                        .requestMatchers("/info").permitAll()
//
//                        // ============================================
//                        // ALL OTHER ENDPOINTS - REQUIRE AUTHENTICATION
//                        // ============================================
//                        // Everything else requires JWT authentication
//                        .anyRequest().authenticated())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll());
                // Add JWT filter
//                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS Configuration
     * Allows requests from frontend on http://localhost:3000
     */
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//
//        // Allow frontend origin
//        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:3001"));
//
//        // Allow common methods
//        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
//
//        // Allow headers needed for JWT auth
//        configuration.setAllowedHeaders(Arrays.asList("*"));
//        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
//
//        // Allow credentials (for cookies if needed)
//        configuration.setAllowCredentials(false);
//
//        // Cache for 1 hour
//        configuration.setMaxAge(3600L);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration);
//        return source;
//    }
}
