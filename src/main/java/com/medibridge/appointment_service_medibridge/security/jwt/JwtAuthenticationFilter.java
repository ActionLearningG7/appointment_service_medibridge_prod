package com.medibridge.appointment_service_medibridge.security.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();

        // Bypass JWT processing for WebSocket endpoints
        // SockJS handshake and STOMP endpoints don't need JWT at HTTP layer
        // STOMP CONNECT authentication handled by WebSocketAuthInterceptor
        if (isWebSocketPath(path)) {
            log.debug("Bypassing JWT for WebSocket path: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                Claims claims = jwtTokenProvider.getClaimsFromToken(jwt);
                String username = claims.get("email", String.class);
                String role = claims.get("role", String.class);
                String userId = claims.getSubject();

                // Validate all required claims are present
                if (username != null && role != null && userId != null) {
                    // Ensure role has ROLE_ prefix
                    String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;

                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                            new SimpleGrantedAuthority(authority));

                    try {
                        UUID userUuid = UUID.fromString(userId);
                        JwtUserPrincipal principal = new JwtUserPrincipal(userUuid, username, authorities);

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities());

                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } catch (IllegalArgumentException e) {
                        log.error("Invalid UUID format for userId in token: {}", userId);
                    }
                } else {
                    log.warn("Token missing required claims: username={}, role={}, userId={}", username, role, userId);
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in appointment service security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Check if path is a WebSocket endpoint that should bypass JWT
     */
    private boolean isWebSocketPath(String path) {
        return path.startsWith("/ws") || path.startsWith("/api/ws");
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
