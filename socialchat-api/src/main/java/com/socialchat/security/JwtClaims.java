package com.socialchat.security;

import lombok.Builder;
import lombok.Getter;

import java.util.Date;
import java.util.List;

/**
 * Immutable record of all JWT claims extracted in a single parse operation.
 * Eliminates multiple parsing of the same token.
 */
@Getter
@Builder
public class JwtClaims {
    private final String tokenId;
    private final String username;
    private final Long userId;
    private final List<String> roles;
    private final String tokenType;
    private final Date expiration;
    private final Date issuedAt;

    public boolean isExpired() {
        return expiration.before(new Date());
    }

    public boolean isRefreshToken() {
        return "refresh".equals(tokenType);
    }

    public boolean isAccessToken() {
        return "access".equals(tokenType);
    }
}
