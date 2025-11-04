package org.splitzy.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splitzy.auth.dto.request.LogOutRequest;
import org.splitzy.auth.dto.request.LoginRequest;
import org.splitzy.auth.dto.request.RefreshTokenRequest;
import org.splitzy.auth.dto.request.RegisterRequest;
import org.splitzy.auth.dto.response.AuthResponse;
import org.splitzy.auth.service.AuthService;
import org.splitzy.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Create a new user account with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request received for email: {}", request.getEmail());

        AuthResponse response = authService.register(request);
        ApiResponse<AuthResponse> apiResponse = ApiResponse.success(response, "User registered successfully");

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for: {}", request.getEmailOrusername());

        AuthResponse response = authService.login(request);
        ApiResponse<AuthResponse> apiResponse = ApiResponse.success(response, "Login successful");

        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/logout")
    @Operation(summary = "User Logout.", description = "Invalidate JWT Token and Logout User")
    public ResponseEntity<ApiResponse<String>> logout(@Valid @RequestBody LogOutRequest request) {
        log.info("Logout request received");
        authService.logout(request);
        ApiResponse<String> apiResponse = ApiResponse.success( "Logged out successfully.","Logout successful");
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Get new access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request received");

        AuthResponse response = authService.refreshToken(request);
        ApiResponse<AuthResponse> apiResponse = ApiResponse.success(response, "Token refreshed successfully");

        return ResponseEntity.ok(apiResponse);
    }
}
