package com.banana.harvest.dto.auth;

import com.banana.harvest.entity.enums.UserRole;
import com.banana.harvest.entity.enums.VendorType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AdminCreateUserResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String phone;
    private UserRole role;
    private VendorType vendorType;
    private String bankName;
    private String accountNumber;
    private String ifscCode;
    private Boolean isActive;
    private LocalDateTime createdAt;
    // One-time generated password for admin to share with the new user
    private String generatedPassword;
}
