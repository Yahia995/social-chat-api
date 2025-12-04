package com.socialchat.security;

import com.socialchat.service.TokenRevocationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenRevocationService tokenRevocationService;

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(BEARER_PREFIX.length());

        jwtService.validateAndParse(jwt)
                .filter(JwtClaims::isAccessToken)
                .filter(claims -> !tokenRevocationService.isTokenRevoked(claims.getTokenId()))
                .ifPresent(claims -> {
                    List<SimpleGrantedAuthority> authorities = claims.getRoles() != null
                            ? claims.getRoles().stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList()
                            : List.of(new SimpleGrantedAuthority("ROLE_USER"));

                    JwtAuthenticationToken authToken = new JwtAuthenticationToken(
                            claims.getUserId(),
                            claims.getUsername(),
                            authorities
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Authenticated user: {}", claims.getUsername());
                });

        filterChain.doFilter(request, response);
    }
}
