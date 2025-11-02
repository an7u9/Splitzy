package org.splitzy.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splitzy.auth.dto.request.LogOutRequest;
import org.splitzy.auth.dto.request.LoginRequest;
import org.splitzy.auth.dto.request.RefreshTokenRequest;
import org.splitzy.auth.dto.request.RegisterRequest;
import org.splitzy.auth.dto.response.AuthResponse;
import org.splitzy.auth.entity.AuthUser;
import org.splitzy.auth.repository.AuthUserRepository;
import org.splitzy.common.exception.BusinessException;
import org.splitzy.common.exception.ResourceNotFoundException;
import org.splitzy.common.exception.ValidationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuthUserRepository authUserRepository;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Attempting to register a new user with email: {}", request.getEmail());

        if(!request.isPasswordMatching()){
            throw new validationException("Password and confirm password should be same.")
        }
        if(!authUserRepository.existsByEmail(request.getEmail())){
            throw new validationException("Email already registered.");
        }
        if(authUserRepository.existsByUsername(request.getUsername())){
            throw new validationException("Username is already taken.");
        }

        AuthUser authUser = AuthUser.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .passwordChangedAt(LocalDateTime.now())
                .build();

        AuthUser authUserSaved = authUserRepository.save(authUser);
        log.info("Successfully registered a new user with ID: {}", authUserSaved.getId());

        String accessToken = jwtTokenService.generateAcessToken(authUserSaved);
        String refreshToken = jwtTokenService.generateRefreshToken(authUserSaved);

        return buildAuthResponse(authUserSaved, accessToken, refreshToken);
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Attempting login for: {}", request.getEmailOrusername());

        AuthUser user = authUserRepository.findByUsernameAndIsActiveTrue(request.getEmailOrusername())
                .orElseThrow(() -> new ValidationException("Invalid email/username or password."));

        if(user.isAccountLocked()){
            throw new BusinessException("Account is locked due to multiple login attempts.");
        }

        if(!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())){
            throw new ValidationException("Invalid password.");
        }

        if(user.getFailedLoginAttempts() > 0){
            user.resetFailedloginAttempts();
            authUserRepository.resetFailedLoginAttempts(user.getId());
        }

        user.setLastLogin(LocalDateTime.now());
        authUserRepository.updateLastLogin(user.getId(), user.getLastLogin());

        log.info("Successfully login for: {}", user.getUsername());

        String accessToken = jwtTokenService.generateAcessToken(user);
        String refreshToken = jwtTokenService.generateRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public void logout(LogOutRequest request) {
        log.info("Attempting to logout");

        try{
            jwtTokenService.blacklistToken(request.getAcessToken());

            if(request.getRefreshToken() != null && !request.getRefreshToken().isBlank()){
                jwtTokenService.blacklistToken(request.getRefreshToken());
            }
            log.info("User logged out successfully");
        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage());
            throw new BusinessException("Logout failed. Please try again.");
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("Attempting token refresh");

        // Validate refresh token
        if (!jwtTokenService.validateRefreshToken(request.getRefreshToken())) {
            throw new ValidationException("Invalid or expired refresh token");
        }

        // Extract user information from refresh token
        Long userId = jwtTokenService.getUserIdFromToken(request.getRefreshToken());
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if user is still active
        if (!user.getIsActive()) {
            throw new BusinessException("User account is deactivated");
        }

        // Generate new access token
        String newAccessToken = jwtTokenService.generateAccessToken(user);
        String newRefreshToken = jwtTokenService.generateRefreshToken(user);

        // Blacklist old refresh token
        jwtTokenService.blacklistToken(request.getRefreshToken());

        log.info("Token refreshed successfully for user: {}", user.getEmail());

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    private void handleFailedLogin(AuthUser user) {
        user.incrementFailedAttempts();
        authUserRepository.updateFailedLoginAttempts(
                user.getId(),
                user.getFailedLoginAttempts(),
                user.getAccountLockedUntil()
        );

        log.warn("Failed login attempt for user: {}. Attempts: {}",
                user.getEmail(), user.getFailedLoginAttempts());
    }

    private AuthResponse buildAuthResponse(AuthUser authUser, String accessToken, String refreshToken) {
        return AuthResponse.builder().accessToken(accessToken).refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenService.getRemainingValidityInSeconds(accessToken))
                .expiresAt(jwtTokenService.getExpirationAsLocalDateTime(accessToken))
                .user(AuthResponse.UserInfo.builder()
                        .id(authUser.getId())
                        .username(authUser.getUsername())
                        .email(authUser.getEmail())
                        .firstName(authUser.getFirstName())
                        .lastName(authUser.getLastName())
                        .role(authUser.getRole().toString())
                        .emailVerified(authUser.getIsEmailVerified())
                        .phoneVerified(authUser.getIsPhoneVerified())
                        .build())
                .build();
    }
}
