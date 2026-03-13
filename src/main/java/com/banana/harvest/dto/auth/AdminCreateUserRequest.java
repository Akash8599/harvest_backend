package com.banana.harvest.dto.auth;

import com.banana.harvest.entity.enums.UserRole;
import com.banana.harvest.entity.enums.VendorType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminCreateUserRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String phone;

    @NotNull(message = "Role is required")
    private UserRole role;

    private VendorType vendorType;

    private String bankName;

    private String accountNumber;

    private String ifscCode;
}
