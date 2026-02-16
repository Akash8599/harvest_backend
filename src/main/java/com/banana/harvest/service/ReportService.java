package com.banana.harvest.service;

import com.banana.harvest.dto.report.*;
import com.banana.harvest.entity.*;
import com.banana.harvest.entity.enums.BatchStatus;
import com.banana.harvest.entity.enums.PaymentStatus;
import com.banana.harvest.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final FarmRepository farmRepository;
    private final BatchRepository batchRepository;
    private final InventoryStockRepository stockRepository;
    private final SaleRepository saleRepository;
    private final BatchCostRepository batchCostRepository;
    private final VendorLedgerRepository vendorLedgerRepository;
    private final LaborCostRepository laborCostRepository;
    private final DailyHarvestReportRepository harvestReportRepository;

    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats() {
        Long totalFarms = farmRepository.countTotalFarms();
        Long totalBatches = batchRepository.countTotalBatches();
        Long activeBatches = batchRepository.countByStatus(BatchStatus.IN_PROGRESS);
        Long completedBatches = batchRepository.countByStatus(BatchStatus.COMPLETED);
        
        Integer totalBoxesInStock = stockRepository.sumAvailableQuantity();
        Integer totalFilledBoxes = batchRepository.sumCompletedBoxes();
        
        BigDecimal totalSales = saleRepository.sumTotalRevenue();
        BigDecimal totalRevenue = totalSales;
        
        BigDecimal averageCostPerBox = batchCostRepository.averageCostPerBox();
        BigDecimal averageSalePrice = saleRepository.averageSalePrice();
        
        // Calculate total profit
        BigDecimal totalCost = batchCostRepository.sumTotalCosts();
        BigDecimal totalProfit = totalRevenue.subtract(totalCost);

        return DashboardStats.builder()
                .totalFarms(totalFarms)
                .totalBatches(totalBatches)
                .activeBatches(activeBatches)
                .completedBatches(completedBatches)
                .totalBoxesInStock(totalBoxesInStock != null ? totalBoxesInStock : 0)
                .totalFilledBoxes(totalFilledBoxes != null ? totalFilledBoxes : 0)
                .totalSales(totalSales)
                .totalRevenue(totalRevenue)
                .averageCostPerBox(averageCostPerBox)
                .averageSalePrice(averageSalePrice)
                .totalProfit(totalProfit)
                .build();
    }

    @Transactional(readOnly = true)
    public List<VendorLedgerResponse> getVendorLedger(UUID vendorId) {
        return vendorLedgerRepository.findByVendorId(vendorId).stream()
                .map(this::mapToVendorLedgerResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VendorBalance getVendorBalance(UUID vendorId) {
        Integer boxesIssued = vendorLedgerRepository.sumBoxesIssuedByVendor(vendorId);
        Integer boxesReturned = vendorLedgerRepository.sumBoxesReturnedByVendor(vendorId);
        
        if (boxesIssued == null) boxesIssued = 0;
        if (boxesReturned == null) boxesReturned = 0;
        
        BigDecimal pendingLaborCost = laborCostRepository.sumPendingAmountByVendor(vendorId);
        if (pendingLaborCost == null) pendingLaborCost = BigDecimal.ZERO;

        return VendorBalance.builder()
                .vendorId(vendorId)
                .boxesIssued(boxesIssued)
                .boxesReturned(boxesReturned)
                .boxesPending(boxesIssued - boxesReturned)
                .pendingLaborCost(pendingLaborCost)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ProfitabilityReportResponse> getProfitabilityReport() {
        List<Sale> sales = saleRepository.findAll();
        
        return sales.stream().map(sale -> {
            Batch batch = sale.getBatch();
            BatchCost batchCost = batchCostRepository.findByBatchId(batch.getId()).orElse(null);
            
            BigDecimal costPerBox = batchCost != null ? batchCost.getFinalCostPerBox() : BigDecimal.ZERO;
            BigDecimal totalCost = costPerBox.multiply(BigDecimal.valueOf(sale.getTotalBoxes()));
            BigDecimal totalRevenue = sale.getGrandTotal();
            BigDecimal netProfit = totalRevenue.subtract(totalCost);
            
            BigDecimal profitMargin = totalRevenue.compareTo(BigDecimal.ZERO) > 0 
                    ? netProfit.multiply(BigDecimal.valueOf(100)).divide(totalRevenue, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            return ProfitabilityReportResponse.builder()
                    .batchId(batch.getId())
                    .batchIdCode(batch.getBatchId())
                    .farmName(batch.getFarm() != null ? batch.getFarm().getFarmerName() : "N/A")
                    .totalBoxes(sale.getTotalBoxes())
                    .costPerBox(costPerBox)
                    .salePricePerBox(sale.getPricePerBox())
                    .totalCost(totalCost)
                    .totalRevenue(totalRevenue)
                    .netProfit(netProfit)
                    .profitMargin(profitMargin)
                    .saleDate(sale.getSaleDate())
                    .buyerName(sale.getBuyerName())
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DailyActivityReport getDailyActivityReport() {
        Integer todayBoxesPacked = harvestReportRepository.sumTodayBoxesPacked();
        if (todayBoxesPacked == null) todayBoxesPacked = 0;
        
        List<DailyHarvestReport> todayReports = harvestReportRepository.findByReportDate(java.time.LocalDate.now());
        
        List<FarmActivity> farmActivities = todayReports.stream()
                .map(r -> FarmActivity.builder()
                        .farmName(r.getBatch().getFarm().getFarmerName())
                        .batchId(r.getBatch().getBatchId())
                        .boxesPacked(r.getBoxesPacked())
                        .laborCount(r.getLaborCount())
                        .build())
                .collect(Collectors.toList());

        return DailyActivityReport.builder()
                .date(java.time.LocalDate.now())
                .totalBoxesPacked(todayBoxesPacked)
                .totalFarmsHarvested(farmActivities.size())
                .farmActivities(farmActivities)
                .build();
    }

    private VendorLedgerResponse mapToVendorLedgerResponse(VendorLedger ledger) {
        return VendorLedgerResponse.builder()
                .id(ledger.getId())
                .vendorId(ledger.getVendor().getId())
                .vendorName(ledger.getVendor().getFullName())
                .batchId(ledger.getBatch() != null ? ledger.getBatch().getId() : null)
                .batchIdCode(ledger.getBatch() != null ? ledger.getBatch().getBatchId() : null)
                .transactionType(ledger.getTransactionType())
                .quantity(ledger.getQuantity())
                .amount(ledger.getAmount())
                .balanceBoxes(ledger.getBalanceBoxes())
                .balanceAmount(ledger.getBalanceAmount())
                .notes(ledger.getNotes())
                .createdAt(ledger.getCreatedAt())
                .build();
    }

    // Inner classes for report DTOs
    @lombok.Data
    @lombok.Builder
    public static class VendorBalance {
        private UUID vendorId;
        private Integer boxesIssued;
        private Integer boxesReturned;
        private Integer boxesPending;
        private BigDecimal pendingLaborCost;
    }

    @lombok.Data
    @lombok.Builder
    public static class DailyActivityReport {
        private java.time.LocalDate date;
        private Integer totalBoxesPacked;
        private Integer totalFarmsHarvested;
        private List<FarmActivity> farmActivities;
    }

    @lombok.Data
    @lombok.Builder
    public static class FarmActivity {
        private String farmName;
        private String batchId;
        private Integer boxesPacked;
        private Integer laborCount;
    }
}
