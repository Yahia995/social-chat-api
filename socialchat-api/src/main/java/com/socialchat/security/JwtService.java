package com.socialchat.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;

@Slf4j
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Getter
    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Getter
    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private SecretKey signInKey;

    /**
     * Parse token once and extract all claims.
     * This is the primary method - use this instead of multiple extract calls.
     *
     * @return JwtClaims or empty Optional if token is invalid
     */
    @SuppressWarnings("unchecked")
    public Optional<JwtClaims> parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Optional.of(JwtClaims.builder()
                    .tokenId(claims.getId())
                    .username(claims.getSubject())
                    .userId(claims.get("userId", Long.class))
                    .roles(claims.get("roles", List.class))
                    .tokenType(claims.get("type", String.class))
                    .expiration(claims.getExpiration())
                    .issuedAt(claims.getIssuedAt())
                    .build());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT parsing failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Validate token and return claims if valid.
     * Combines parsing + validation in one call.
     */
    public Optional<JwtClaims> validateAndParse(String token) {
        return parseToken(token)
                .filter(claims -> !claims.isExpired());
    }

    public String generateAccessToken(Long userId, String username, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("roles", roles);
        claims.put("type", "access");
        return buildToken(claims, username, accessTokenExpiration);
    }

    public String generateRefreshToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");
        return buildToken(claims, username, refreshTokenExpiration);
    }

    private String buildToken(Map<String, Object> claims, String username, long expiration) {
        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .id(UUID.randomUUID().toString())
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    private SecretKey getSignInKey() {
        if (signInKey == null) {
            byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
            signInKey = Keys.hmacShaKeyFor(keyBytes);
        }
        return signInKey;
    }

    // ============ Legacy methods for backward compatibility ============
    // These delegate to parseToken() internally

   @Deprecated
    public String extractTokenId(String token) {
        return parseToken(token).map(JwtClaims::getTokenId).orElse(null);
    }

    @Deprecated
    public Date extractExpirationDate(String token) {
        return parseToken(token).map(JwtClaims::getExpiration).orElse(null);
    }

    @Deprecated
    public boolean isTokenValid(String token) {
        return validateAndParse(token).isPresent();
    }

    @Deprecated
    public boolean isRefreshToken(String token) {
        return parseToken(token).map(JwtClaims::isRefreshToken).orElse(false);
    }
}
