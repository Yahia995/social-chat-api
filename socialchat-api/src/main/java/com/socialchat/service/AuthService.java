package com.socialchat.service;

import com.socialchat.dto.auth.*;
import com.socialchat.dto.user.UserResponse;
import com.socialchat.entity.User;
import com.socialchat.exception.BadRequestException;
import com.socialchat.exception.ConflictException;
import com.socialchat.exception.UnauthorizedException;
import com.socialchat.mapper.UserMapper;
import com.socialchat.repository.UserRepository;
import com.socialchat.security.JwtClaims;
import com.socialchat.security.JwtService;
import com.socialchat.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final TokenRevocationService tokenRevocationService;
    private final SecurityUtils securityUtils;

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("USERNAME_EXISTS", "Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("EMAIL_EXISTS", "Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName() != null ? request.getDisplayName() : request.getUsername())
                .build();

        user = userRepository.save(user);
        log.info("User registered: {}", user.getUsername());

        return generateTokenResponse(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        user.setOnline(true);
        user.setLastSeen(LocalDateTime.now());
        userRepository.save(user);

        log.info("User logged in: {}", user.getUsername());
        return generateTokenResponse(user);
    }

    @Transactional
    public TokenResponse refreshToken(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        JwtClaims claims = jwtService.validateAndParse(refreshToken)
                .filter(JwtClaims::isRefreshToken)
                .filter(c -> !tokenRevocationService.isTokenRevoked(c.getTokenId()))
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        User user = userRepository.findById(claims.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        // Revoke old refresh token
        tokenRevocationService.revokeToken(claims, user.getId());

        log.info("Token refreshed for user: {}", user.getUsername());
        return generateTokenResponse(user);
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        jwtService.parseToken(accessToken).ifPresent(claims -> {
            User user = userRepository.findById(claims.getUserId()).orElse(null);

            if (user != null) {
                tokenRevocationService.revokeToken(claims, user.getId());

                if (refreshToken != null && !refreshToken.isEmpty()) {
                    jwtService.parseToken(refreshToken)
                            .ifPresent(rc -> tokenRevocationService.revokeToken(rc, user.getId()));
                }

                user.setOnline(false);
                user.setLastSeen(LocalDateTime.now());
                userRepository.save(user);

                log.info("User logged out: {}", user.getUsername());
            }
        });
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request, String currentAccessToken) {
        User user = securityUtils.getCurrentUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("INVALID_PASSWORD", "Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        jwtService.parseToken(currentAccessToken)
                .ifPresent(claims -> tokenRevocationService.revokeToken(claims, user.getId()));

        log.info("Password changed for user: {}", user.getUsername());
    }

    private TokenResponse generateTokenResponse(User user) {
        List<String> roles = List.of("ROLE_USER");

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), roles);
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getUsername());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration() / 1000)
                .user(userMapper.toResponse(user))
                .build();
    }
}
