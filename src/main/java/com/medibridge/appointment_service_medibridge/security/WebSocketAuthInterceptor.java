package com.medibridge.appointment_service_medibridge.security;

import com.medibridge.appointment_service_medibridge.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * WebSocket Authentication Interceptor
 * 
 * Authenticates WebSocket connections using JWT tokens from:
 * 1. STOMP CONNECT headers (Authorization: Bearer <token>)
 * 2. Query parameters (?token=<token>) - fallback for browsers
 * 3. Gateway-forwarded headers (X-User-Id, X-User-Email, X-User-Role)
 * 
 * This interceptor runs on EVERY WebSocket message to ensure continuous
 * authentication
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        log.info("🔌 [WebSocketAuthInterceptor] preSend called");
        
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            log.warn("⚠️ [WebSocketAuthInterceptor] No StompHeaderAccessor found");
            return message;
        }

        StompCommand command = accessor.getCommand();
        log.info("🔌 [WebSocketAuthInterceptor] STOMP Command: {}", command);

        if (command == null) {
            log.warn("⚠️ [WebSocketAuthInterceptor] No STOMP command found");
            return message;
        }

        if (StompCommand.CONNECT.equals(command)) {
            log.info("=== 🔐 WebSocket CONNECT attempt ===");
            log.info("Headers: {}", accessor.toString());
            
            // Extract token from multiple sources
            String token = extractToken(accessor);

            if (token != null) {
                log.info("✅ Token found, length: {}", token.length());
                log.info("🔐 Validating token...");
                
                if (jwtTokenProvider.validateToken(token)) {
                    try {
                        String email = jwtTokenProvider.getEmailFromToken(token);
                        String role = jwtTokenProvider.getRoleFromToken(token);
                        String userId = jwtTokenProvider.getUserIdFromToken(token).toString();

                        log.info("✅ Token validated - userId={}, email={}, role={}", userId, email, role);

                        // Create authentication with userId as principal name (so it can be extracted as UUID)
                        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                                new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));

                        // Use userId as the principal name instead of email
                        // This allows extractUserId to parse it as UUID
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId,
                                null, authorities);

                        // Set user in WebSocket session
                        accessor.setUser(authentication);

                        // Add custom attributes for easy access in controllers
                        accessor.getSessionAttributes().put("userId", userId);
                        accessor.getSessionAttributes().put("email", email);
                        accessor.getSessionAttributes().put("role", role);

                        log.info("✅ WebSocket authenticated: userId={}, email={}, role={}", userId, email, role);

                    } catch (Exception e) {
                        log.error("❌ Error authenticating WebSocket connection: {}", e.getMessage(), e);
                        throw new IllegalArgumentException("Invalid authentication token: " + e.getMessage());
                    }
                } else {
                    log.error("❌ Token validation failed");
                    throw new IllegalArgumentException("Invalid authentication token");
                }
            } else {
                log.error("❌ WebSocket connection attempt without token");
                throw new IllegalArgumentException("Missing authentication token");
            }
        }

        return message;
    }

    /**
     * Extract JWT token from multiple sources (in priority order):
     * 1. Authorization header (Bearer token)
     * 2. Gateway-forwarded X-User-Id header (already authenticated by gateway)
     * 3. Query parameter (?token=xxx)
     */
    private String extractToken(StompHeaderAccessor accessor) {
        // 1. Check Authorization header (standard approach)
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }

        // 2. Check if already authenticated by gateway (X-User-Id header present)
        List<String> userIdHeaders = accessor.getNativeHeader("X-User-Id");
        if (userIdHeaders != null && !userIdHeaders.isEmpty()) {
            // Gateway already validated the token, trust the gateway
            // Reconstruct authentication from gateway headers
            List<String> emailHeaders = accessor.getNativeHeader("X-User-Email");
            List<String> roleHeaders = accessor.getNativeHeader("X-User-Role");

            if (emailHeaders != null && roleHeaders != null) {
                String userId = userIdHeaders.get(0);
                String email = emailHeaders.get(0);
                String role = roleHeaders.get(0);

                // Set authentication directly without token validation
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(email,
                        null, authorities);

                accessor.setUser(authentication);
                accessor.getSessionAttributes().put("userId", userId);
                accessor.getSessionAttributes().put("email", email);
                accessor.getSessionAttributes().put("role", role);

                log.info("WebSocket authenticated via gateway headers: user={}, role={}", email, role);
                return null; // Return null to skip token validation (already validated by gateway)
            }
        }

        // 3. Check query parameter (fallback for browsers that don't support custom
        // headers)
        List<String> tokenParams = accessor.getNativeHeader("token");
        if (tokenParams != null && !tokenParams.isEmpty()) {
            return tokenParams.get(0);
        }

        return null;
    }
}
