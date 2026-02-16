package com.banana.harvest.dto.cost;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class BatchCostResponse {
    private UUID id;
    private UUID batchId;
    private String batchIdCode;
    private BigDecimal materialCostTotal;
    private BigDecimal materialCostPerBox;
    private BigDecimal outwardTransportCost;
    private BigDecimal outwardTransportPerBox;
    private BigDecimal laborCostTotal;
    private BigDecimal laborCostPerBox;
    private BigDecimal inwardTransportCost;
    private BigDecimal inwardTransportPerBox;
    private BigDecimal totalCost;
    private BigDecimal finalCostPerBox;
    private LocalDateTime calculatedAt;
}
