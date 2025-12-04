package com.socialchat.security;

import com.socialchat.entity.User;
import com.socialchat.exception.UnauthorizedException;
import com.socialchat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility class for accessing current user information from JWT.
 * No database calls for userId/username - extracted directly from JWT claims.
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    public Long getCurrentUserId() {
        return getJwtAuthentication().getUserId();
    }

    public String getCurrentUsername() {
        return getJwtAuthentication().getUsername();
    }

    /**
     * Get full User entity - this DOES make a database call.
     * Use only when you need the complete User object.
     */
    public User getCurrentUser() {
        Long userId = getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    /**
     * Check if current user is authenticated.
     */
    public boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth instanceof JwtAuthenticationToken && auth.isAuthenticated();
    }

    private JwtAuthenticationToken getJwtAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth;
        }

        throw new UnauthorizedException("User not authenticated");
    }
}
