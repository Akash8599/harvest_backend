package com.banana.harvest.dto.harvest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DailyHarvestRequest {
    
    @NotBlank(message = "Batch ID is required")
    private String batchId;
    
    @NotNull(message = "Report date is required")
    private LocalDate reportDate;
    
    @NotNull(message = "Boxes packed is required")
    private Integer boxesPacked;
    
    @NotNull(message = "Boxes wasted is required")
    private Integer boxesWasted;
    
    @NotNull(message = "Labor count is required")
    private Integer laborCount;
    
    private String notes;
    
    private BigDecimal laborCost;
}
