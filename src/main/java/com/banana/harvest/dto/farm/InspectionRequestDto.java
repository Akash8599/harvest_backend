package com.banana.harvest.dto.farm;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InspectionRequestDto {
    
    @NotBlank(message = "Farm ID is required")
    private String farmId;
    
    @NotBlank(message = "Vendor ID is required")
    private String vendorId;
    
    private String notes;
}
