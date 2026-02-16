package com.banana.harvest.dto.auth;

import com.banana.harvest.entity.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String phone;
    private UserRole role;
    private Boolean isActive;
    private String profileImageUrl;
    private LocalDateTime createdAt;
}
