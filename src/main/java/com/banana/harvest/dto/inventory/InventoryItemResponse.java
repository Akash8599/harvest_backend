package com.banana.harvest.dto.inventory;

import com.banana.harvest.entity.enums.InventoryCategory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class InventoryItemResponse {
    private UUID id;
    private String itemName;
    private String itemCode;
    private InventoryCategory category;
    private String unitOfMeasure;
    private BigDecimal unitCost;
    private Boolean isActive;
    private Integer availableQuantity;
    private LocalDateTime createdAt;
}
