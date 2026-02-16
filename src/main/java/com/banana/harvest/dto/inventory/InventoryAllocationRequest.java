package com.banana.harvest.dto.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryAllocationRequest {
    
    @NotBlank(message = "Batch ID is required")
    private String batchId;
    
    @NotBlank(message = "Item ID is required")
    private String itemId;
    
    @NotNull(message = "Quantity is required")
    private Integer quantity;
    
    private String notes;
}
