package com.banana.harvest.dto.harvest;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DailyHarvestResponse {
    private UUID id;
    private UUID batchId;
    private String batchIdCode;
    private LocalDate reportDate;
    private Integer boxesPacked;
    private Integer boxesWasted;
    private Integer laborCount;
    private String notes;
    private BigDecimal laborCost;
    private String laborPaymentStatus;
    private LocalDateTime createdAt;
}
