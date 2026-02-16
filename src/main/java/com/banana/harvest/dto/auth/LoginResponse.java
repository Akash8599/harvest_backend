package com.banana.harvest.dto.auth;

import com.banana.harvest.entity.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String refreshToken;
    private UUID userId;
    private String email;
    private String fullName;
    private UserRole role;
    private Boolean isActive;
    private String profileImageUrl;
    private Long expiresIn;
}
