package com.banana.harvest.controller;

import com.banana.harvest.dto.auth.*;
import com.banana.harvest.dto.common.ApiResponse;
import com.banana.harvest.security.UserPrincipal;
import com.banana.harvest.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and user management APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        LoginResponse response = authService.login(request);
        log.info("Login successful for userId: {}, email: {}, role: {}", response.getUserId(), response.getEmail(), response.getRole());
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/register")
//    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Register new user", description = "Create a new user account (Admin/Manager only)")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("User registration request for email: {}, role: {}", request.getEmail(), request.getRole());
        UserResponse response = authService.register(request);
        log.info("User registered successfully - userId: {}, email: {}, role: {}", response.getId(), response.getEmail(), response.getRole());
        return ResponseEntity.ok(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Get new access token using refresh token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.debug("Token refresh request received");
        LoginResponse response = authService.refreshToken(request);
        log.info("Token refreshed successfully for userId: {}", response.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get details of currently logged in user")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.debug("Get current user request for userId: {}", userPrincipal.getId());
        UserResponse response = authService.getCurrentUser(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Get all users", description = "Get list of all users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        log.info("Fetching all users");
        List<UserResponse> response = authService.getAllUsers();
        log.debug("Retrieved {} users", response.size());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/users/role/{role}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Get users by role", description = "Get users filtered by role")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByRole(@PathVariable String role) {
        log.info("Fetching users by role: {}", role);
        List<UserResponse> response = authService.getUsersByRole(role);
        log.debug("Retrieved {} users with role: {}", response.size(), role);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/approve/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Approve user", description = "Activate a pending user account (Super Admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> approveUser(@PathVariable UUID userId) {
        log.info("User approval request for userId: {}", userId);
        UserResponse response = authService.approveUser(userId);
        log.info("User approved successfully - userId: {}, email: {}", response.getId(), response.getEmail());
        return ResponseEntity.ok(ApiResponse.success("User approved successfully", response));
    }

    @PostMapping("/deactivate/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Deactivate user", description = "Deactivate an active user account (Super Admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(@PathVariable UUID userId) {
        log.info("User deactivation request for userId: {}", userId);
        UserResponse response = authService.deactivateUser(userId);
        log.info("User deactivated successfully - userId: {}, email: {}", response.getId(), response.getEmail());
        return ResponseEntity.ok(ApiResponse.success("User deactivated successfully", response));
    }

    @GetMapping("/users/pending")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Get pending users", description = "Get list of users pending approval")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getPendingUsers() {
        log.info("Fetching pending users");
        List<UserResponse> response = authService.getPendingUsers();
        log.debug("Retrieved {} pending users", response.size());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
