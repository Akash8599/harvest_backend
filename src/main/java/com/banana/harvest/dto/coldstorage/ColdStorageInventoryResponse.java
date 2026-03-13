package com.banana.harvest.dto.coldstorage;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ColdStorageInventoryResponse {
    private Integer totalInwardBoxes;
    private Integer totalOutwardBoxes;
    private Integer balanceBoxes;
}
