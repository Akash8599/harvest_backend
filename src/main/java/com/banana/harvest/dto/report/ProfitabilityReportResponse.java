package com.banana.harvest.dto.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class ProfitabilityReportResponse {
    private UUID batchId;
    private String batchIdCode;
    private String farmName;
    private Integer totalBoxes;
    private BigDecimal costPerBox;
    private BigDecimal salePricePerBox;
    private BigDecimal totalCost;
    private BigDecimal totalRevenue;
    private BigDecimal netProfit;
    private BigDecimal profitMargin;
    private LocalDate saleDate;
    private String buyerName;
}
