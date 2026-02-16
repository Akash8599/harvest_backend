package com.banana.harvest.dto.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DashboardStats {
    private Long totalFarms;
    private Long totalBatches;
    private Long activeBatches;
    private Long completedBatches;
    private Integer totalBoxesInStock;
    private Integer totalFilledBoxes;
    private BigDecimal totalSales;
    private BigDecimal totalRevenue;
    private BigDecimal averageCostPerBox;
    private BigDecimal averageSalePrice;
    private BigDecimal totalProfit;
}
