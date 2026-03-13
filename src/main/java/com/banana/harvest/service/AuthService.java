package com.banana.harvest.service;

import com.banana.harvest.dto.auth.*;
import com.banana.harvest.entity.User;
import com.banana.harvest.exception.BusinessException;
import com.banana.harvest.exception.ResourceNotFoundException;
import com.banana.harvest.repository.UserRepository;
import com.banana.harvest.security.JwtUtil;
import com.banana.harvest.security.UserPrincipal;
import com.banana.harvest.entity.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Processing login for email: {}", request.getEmail());

        // First check if user exists and is active
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Invalid email or password"));

        if (!user.getIsActive()) {
            log.warn("Login attempt for inactive account: email={}", request.getEmail());
            throw new BusinessException("Account pending approval. Please contact administrator.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            log.debug("Authentication successful for userId: {}, generating tokens", userPrincipal.getId());

            String token = jwtUtil.generateToken(authentication);
            String refreshToken = jwtUtil.generateRefreshToken(userPrincipal.getId());

            log.info("Login completed successfully for userId: {}, email: {}, role: {}",
                    userPrincipal.getId(), userPrincipal.getEmail(), userPrincipal.getRole());

            return LoginResponse.builder()
                    .token(token)
                    .refreshToken(refreshToken)
                    .userId(userPrincipal.getId())
                    .email(userPrincipal.getEmail())
                    .fullName(userPrincipal.getFullName())
                    .role(userPrincipal.getRole())
                    .isActive(userPrincipal.getIsActive())
                    .expiresIn(jwtUtil.getExpirationTime())
                    .build();
        } catch (Exception e) {
            log.error("Login failed for email: {} - Error: {}", request.getEmail(), e.getMessage());
            throw e;
        }
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        log.info("Processing user registration for email: {}, role: {}", request.getEmail(), request.getRole());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed - Email already exists: {}", request.getEmail());
            throw new BusinessException("Email already registered");
        }

        // Set isActive based on role - SUPER_ADMIN is active by default, others need approval
        boolean isActive = request.getRole().name().equals("SUPER_ADMIN");

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(request.getRole())
                .isActive(isActive)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully - userId: {}, email: {}, role: {}, isActive: {}",
                savedUser.getId(), savedUser.getEmail(), savedUser.getRole(), savedUser.getIsActive());

        return mapToUserResponse(savedUser);
    }

    // -------------------------------------------------------------------
    // Admin: create user with auto-generated password
    // -------------------------------------------------------------------
    @Transactional
    public AdminCreateUserResponse adminCreateUser(AdminCreateUserRequest request) {
        log.info("Admin creating user - email: {}, role: {}", request.getEmail(), request.getRole());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered");
        }

        String generatedPassword = generateRandomPassword(10);

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(generatedPassword))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(request.getRole())
                .vendorType(request.getVendorType())
                .bankName(request.getBankName())
                .accountNumber(request.getAccountNumber())
                .ifscCode(request.getIfscCode())
                .isActive(true) // Admin-created accounts are active immediately
                .build();

        User savedUser = userRepository.save(user);
        log.info("Admin created user - userId: {}, email: {}, role: {}", savedUser.getId(), savedUser.getEmail(), savedUser.getRole());

        return AdminCreateUserResponse.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .phone(savedUser.getPhone())
                .role(savedUser.getRole())
                .vendorType(savedUser.getVendorType())
                .bankName(savedUser.getBankName())
                .accountNumber(savedUser.getAccountNumber())
                .ifscCode(savedUser.getIfscCode())
                .isActive(savedUser.getIsActive())
                .createdAt(savedUser.getCreatedAt())
                .generatedPassword(generatedPassword)
                .build();
    }

    // -------------------------------------------------------------------
    // Admin: update user details
    // -------------------------------------------------------------------
    @Transactional
    public UserResponse updateUser(UUID userId, AdminCreateUserRequest request) {
        log.info("Updating user - userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Check email uniqueness if changing email
        if (!user.getEmail().equalsIgnoreCase(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered to another user");
        }

        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());
        user.setVendorType(request.getVendorType());
        user.setBankName(request.getBankName());
        user.setAccountNumber(request.getAccountNumber());
        user.setIfscCode(request.getIfscCode());

        User savedUser = userRepository.save(user);
        log.info("User updated successfully - userId: {}", userId);
        return mapToUserResponse(savedUser);
    }

    @Transactional
    public UserResponse approveUser(UUID userId) {
        log.info("Processing user approval for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.getIsActive()) {
            log.warn("User already active - userId: {}", userId);
            throw new BusinessException("User is already active");
        }

        user.setIsActive(true);
        User savedUser = userRepository.save(user);

        log.info("User approved successfully - userId: {}, email: {}", userId, user.getEmail());

        return mapToUserResponse(savedUser);
    }

    @Transactional
    public UserResponse deactivateUser(UUID userId) {
        log.info("Processing user deactivation for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!user.getIsActive()) {
            log.warn("User already inactive - userId: {}", userId);
            throw new BusinessException("User is already inactive");
        }

        user.setIsActive(false);
        User savedUser = userRepository.save(user);

        log.info("User deactivated successfully - userId: {}, email: {}", userId, user.getEmail());

        return mapToUserResponse(savedUser);
    }

    @Transactional
    public UserResponse activateUser(UUID userId) {
        log.info("Processing user activation for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.getIsActive()) {
            throw new BusinessException("User is already active");
        }

        user.setIsActive(true);
        User savedUser = userRepository.save(user);

        log.info("User activated successfully - userId: {}, email: {}", userId, user.getEmail());
        return mapToUserResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getPendingUsers() {
        log.info("Fetching pending users from database");
        List<User> users = userRepository.findByIsActive(false);
        log.info("Found {} pending users in database", users.size());

        List<UserResponse> userResponses = users.stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());

        userResponses.forEach(user ->
            log.info("Pending User: id={}, email={}, fullName={}, role={}, createdAt={}",
                user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getCreatedAt())
        );

        log.info("Successfully retrieved {} pending users", userResponses.size());
        return userResponses;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        log.info("Fetching current user details for userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        UserResponse response = mapToUserResponse(user);
        log.info("Current User: id={}, email={}, fullName={}, role={}, isActive={}",
            response.getId(), response.getEmail(), response.getFullName(),
            response.getRole(), response.getIsActive());

        return response;
    }

    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        log.debug("Processing token refresh request");

        if (!jwtUtil.validateToken(request.getRefreshToken())) {
            log.warn("Token refresh failed - Invalid refresh token");
            throw new BusinessException("Invalid refresh token");
        }

        UUID userId = jwtUtil.getUserIdFromToken(request.getRefreshToken());
        log.debug("Refresh token validated for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!user.getIsActive()) {
            log.warn("Token refresh failed - User account deactivated: userId={}", userId);
            throw new BusinessException("User account is deactivated");
        }

        String newToken = jwtUtil.generateTokenFromUserId(userId);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId);

        log.info("Token refreshed successfully for userId: {}, email: {}", userId, user.getEmail());

        return LoginResponse.builder()
                .token(newToken)
                .refreshToken(newRefreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .expiresIn(jwtUtil.getExpirationTime())
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users from database");
        List<User> users = userRepository.findAll();
        log.info("Found {} users in database", users.size());

        List<UserResponse> userResponses = users.stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());

        userResponses.forEach(user ->
            log.info("User: id={}, email={}, fullName={}, role={}, isActive={}",
                user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getIsActive())
        );

        log.info("Successfully retrieved {} users", userResponses.size());
        return userResponses;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByRole(String role) {
        log.info("Fetching users by role: {}", role);
        UserRole userRole = UserRole.valueOf(role.toUpperCase());
        List<User> users = userRepository.findByRole(userRole);

        log.info("Found {} users with role: {}", users.size(), role);

        List<UserResponse> userResponses = users.stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());

        userResponses.forEach(user ->
            log.info("User with role {}: id={}, email={}, fullName={}, isActive={}",
                role, user.getId(), user.getEmail(), user.getFullName(), user.getIsActive())
        );

        log.info("Successfully retrieved {} users with role: {}", userResponses.size(), role);
        return userResponses;
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .vendorType(user.getVendorType())
                .bankName(user.getBankName())
                .accountNumber(user.getAccountNumber())
                .ifscCode(user.getIfscCode())
                .profileImageUrl(user.getProfileImageUrl())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String generateRandomPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }
}
