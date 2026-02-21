package com.banana.harvest.dto.farm;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FarmRequest {
    
    @NotBlank(message = "Farmer name is required")
    private String farmerName;
    
    @NotBlank(message = "Location is required")
    private String location;
    
    private BigDecimal latitude;
    
    private BigDecimal longitude;
    
    private String contactNumber;
    
    private BigDecimal totalArea;
    
    private String areaUnit = "acres";
    
    @NotBlank(message = "Produce type is required")
    private String produceType;
}
