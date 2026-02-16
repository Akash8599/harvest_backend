package com.banana.harvest.dto.sales;

import com.banana.harvest.entity.enums.SaleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SaleRequest {
    
    @NotBlank(message = "Batch ID is required")
    private String batchId;
    
    @NotBlank(message = "Buyer name is required")
    private String buyerName;
    
    private String buyerContact;
    
    private String buyerAddress;
    
    @NotNull(message = "Sale type is required")
    private SaleType saleType;
    
    @NotNull(message = "Total boxes is required")
    private Integer totalBoxes;
    
    @NotNull(message = "Price per box is required")
    private BigDecimal pricePerBox;
    
    private String currency = "INR";
    
    private BigDecimal exchangeRate;
    
    private BigDecimal taxPercentage;
    
    @NotNull(message = "Sale date is required")
    private LocalDate saleDate;
}
