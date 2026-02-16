package com.banana.harvest.dto.harvest;

import com.banana.harvest.entity.enums.TransportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransportCostRequest {
    
    @NotBlank(message = "Batch ID is required")
    private String batchId;
    
    @NotNull(message = "Transport type is required")
    private TransportType costType;
    
    private String vendorName;
    
    private String vehicleNumber;
    
    private String driverName;
    
    private String driverPhone;
    
    @NotNull(message = "Total cost is required")
    private BigDecimal totalCost;
    
    private BigDecimal distanceKm;
    
    private String notes;
}
