package com.banana.harvest.dto.inventory;

import com.banana.harvest.entity.enums.InventoryCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryItemRequest {
    
    @NotBlank(message = "Item name is required")
    private String itemName;
    
    @NotBlank(message = "Item code is required")
    private String itemCode;
    
    @NotNull(message = "Category is required")
    private InventoryCategory category;
    
    @NotBlank(message = "Unit of measure is required")
    private String unitOfMeasure;
    
    @NotNull(message = "Unit cost is required")
    private BigDecimal unitCost;
}
