package com.banana.harvest.dto.farm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FarmRequest {
    
    @NotBlank(message = "Farmer name is required")
    private String farmerName;
    
    @NotBlank(message = "Location is required")
    private String location;
    
    @NotNull(message = "Latitude is required")
    private BigDecimal latitude;
    
    @NotNull(message = "Longitude is required")
    private BigDecimal longitude;
    
    private String contactNumber;
    
    private BigDecimal totalArea;
    
    private String areaUnit = "acres";
    
    @NotBlank(message = "Produce type is required")
    private String produceType;
}
