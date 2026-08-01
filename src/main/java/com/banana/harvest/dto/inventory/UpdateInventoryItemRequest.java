package com.banana.harvest.dto.inventory;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInventoryItemRequest {
    
    @NotBlank(message = "Item name is required")
    private String itemName;
    
    @NotBlank(message = "Item code is required")
    private String itemCode;
}
