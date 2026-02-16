package com.banana.harvest.dto.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class VendorLedgerResponse {
    private UUID id;
    private UUID vendorId;
    private String vendorName;
    private UUID batchId;
    private String batchIdCode;
    private String transactionType;
    private Integer quantity;
    private BigDecimal amount;
    private Integer balanceBoxes;
    private BigDecimal balanceAmount;
    private String notes;
    private LocalDateTime createdAt;
}
