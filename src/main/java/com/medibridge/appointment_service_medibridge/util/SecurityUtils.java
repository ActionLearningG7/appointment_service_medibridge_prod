package com.medibridge.appointment_service_medibridge.util;

import lombok.experimental.UtilityClass;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import com.medibridge.appointment_service_medibridge.security.jwt.JwtUserPrincipal;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@UtilityClass
public class SecurityUtils {

    /**
     * Get the authenticated user's ID
     * Expects JWT 'sub' claim to be the UUID or a custom claim 'userId'
     */
    public static UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwt) {
            String sub = jwt.getToken().getSubject();
            try {
                return UUID.fromString(sub);
            } catch (IllegalArgumentException e) {
                // Try custom claim if sub is not UUID (e.g. email)
                String userId = jwt.getToken().getClaimAsString("userId");
                if (userId != null) {
                    return UUID.fromString(userId);
                }
            }
        }

        if (authentication instanceof UsernamePasswordAuthenticationToken usernamePasswordAuth) {
            Object principal = usernamePasswordAuth.getPrincipal();
            if (principal instanceof JwtUserPrincipal jwtUser) {
                return jwtUser.getId();
            }
            if (principal instanceof String principalString) {
                return UUID.fromString(principalString);
            }
        }

        // Fallback or explicit error for unauthenticated
        throw new IllegalStateException("User is not authenticated or user ID is invalid");
    }

    /**
     * Get the authenticated user's role
     * Assumes roles are stored in 'realm_access.roles' or standard Spring Security
     * authorities
     */
    public static String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return "SYSTEM";
        }

        // Extract first authority as role (convention)
        // Spring Security usually converts JWT roles to "ROLE_DOCTOR", "ROLE_PATIENT"
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .findFirst()
                .map(r -> r.replace("ROLE_", ""))
                .orElse("UNKNOWN");
    }
}
