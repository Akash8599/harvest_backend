package com.banana.harvest.dto.farm;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InspectionRequestDto {
    
    @NotBlank(message = "Farm ID is required")
    private String farmId;
    
    @NotBlank(message = "Vendor ID is required")
    private String vendorId;
    
    private String notes;

    private LocalDate visitDate;
    private String placeOfVisit;
    private String visitorName;
    private String visitorContact;
    private BigDecimal proposedRate;
}
