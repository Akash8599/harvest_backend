package com.banana.harvest.service;

import com.banana.harvest.dto.report.VendorLedgerResponse;
import com.banana.harvest.entity.*;
import com.banana.harvest.exception.BusinessException;
import com.banana.harvest.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for inventory reconciliation and vendor balance tracking
 * Tracks boxes issued, returned, damaged, and pending
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryReconciliationService {

    private final VendorLedgerRepository ledgerRepository;
    private final InventoryAllocationRepository allocationRepository;
    private final DailyHarvestReportRepository harvestReportRepository;
    private final LaborCostRepository laborCostRepository;
    private final GatePassRepository gatePassRepository;
    private final UserRepository userRepository;

    /**
     * Records box issuance to vendor
     */
    @Transactional
    public void recordBoxIssuance(UUID vendorId, UUID batchId, Integer quantity, String notes) {
        User vendor = userRepository.findById(vendorId)
                .orElseThrow(() -> new BusinessException("Vendor not found"));

        Batch batch = null;
        if (batchId != null) {
            // Batch lookup would go here
        }

        // Calculate new balance
        VendorBalance currentBalance = calculateVendorBalance(vendorId);
        int newBalance = currentBalance.getBoxesPending() + quantity;

        VendorLedger ledger = VendorLedger.builder()
                .vendor(vendor)
                .batch(batch)
                .transactionType("BOX_ISSUED")
                .quantity(quantity)
                .balanceBoxes(newBalance)
                .notes(notes != null ? notes : "Boxes issued to vendor")
                .build();

        ledgerRepository.save(ledger);
        log.info("Recorded box issuance: Vendor={}, Quantity={}, New Balance={}", 
                vendorId, quantity, newBalance);
    }

    /**
     * Records box return from vendor (filled boxes)
     */
    @Transactional
    public void recordBoxReturn(UUID vendorId, UUID batchId, Integer quantity, String notes) {
        User vendor = userRepository.findById(vendorId)
                .orElseThrow(() -> new BusinessException("Vendor not found"));

        // Validate vendor has enough pending boxes
        VendorBalance balance = calculateVendorBalance(vendorId);
        if (quantity > balance.getBoxesPending()) {
            throw new BusinessException(
                String.format("Cannot return %d boxes. Vendor only has %d boxes pending.",
                        quantity, balance.getBoxesPending()),
                "INSUFFICIENT_BOXES"
            );
        }

        int newBalance = balance.getBoxesPending() - quantity;

        VendorLedger ledger = VendorLedger.builder()
                .vendor(vendor)
                .transactionType("BOX_RETURNED")
                .quantity(quantity)
                .balanceBoxes(newBalance)
                .notes(notes != null ? notes : "Filled boxes returned")
                .build();

        ledgerRepository.save(ledger);
        log.info("Recorded box return: Vendor={}, Quantity={}, New Balance={}", 
                vendorId, quantity, newBalance);
    }

    /**
     * Records damaged boxes
     */
    @Transactional
    public void recordDamagedBoxes(UUID vendorId, UUID batchId, Integer quantity, String notes) {
        User vendor = userRepository.findById(vendorId)
                .orElseThrow(() -> new BusinessException("Vendor not found"));

        VendorBalance balance = calculateVendorBalance(vendorId);
        int newBalance = balance.getBoxesPending() - quantity;

        VendorLedger ledger = VendorLedger.builder()
                .vendor(vendor)
                .transactionType("BOX_DAMAGED")
                .quantity(quantity)
                .balanceBoxes(newBalance)
                .notes(notes != null ? notes : "Boxes damaged/wasted")
                .build();

        ledgerRepository.save(ledger);
        log.info("Recorded damaged boxes: Vendor={}, Quantity={}, New Balance={}", 
                vendorId, quantity, newBalance);
    }

    /**
     * Records labor cost for vendor
     */
    @Transactional
    public void recordLaborCost(UUID vendorId, UUID batchId, BigDecimal amount, String notes) {
        User vendor = userRepository.findById(vendorId)
                .orElseThrow(() -> new BusinessException("Vendor not found"));

        VendorBalance balance = calculateVendorBalance(vendorId);
        BigDecimal newBalance = balance.getPendingLaborCost().add(amount);

        VendorLedger ledger = VendorLedger.builder()
                .vendor(vendor)
                .transactionType("LABOR_COST")
                .amount(amount)
                .balanceAmount(newBalance)
                .notes(notes != null ? notes : "Labor cost added")
                .build();

        ledgerRepository.save(ledger);
        log.info("Recorded labor cost: Vendor={}, Amount={}, New Balance={}", 
                vendorId, amount, newBalance);
    }

    /**
     * Records labor payment to vendor
     */
    @Transactional
    public void recordLaborPayment(UUID vendorId, BigDecimal amount, String notes) {
        User vendor = userRepository.findById(vendorId)
                .orElseThrow(() -> new BusinessException("Vendor not found"));

        VendorBalance balance = calculateVendorBalance(vendorId);
        
        if (amount.compareTo(balance.getPendingLaborCost()) > 0) {
            throw new BusinessException(
                String.format("Payment amount (%.2f) exceeds pending labor cost (%.2f)",
                        amount, balance.getPendingLaborCost()),
                "OVERPAYMENT"
            );
        }

        BigDecimal newBalance = balance.getPendingLaborCost().subtract(amount);

        VendorLedger ledger = VendorLedger.builder()
                .vendor(vendor)
                .transactionType("LABOR_PAYMENT")
                .amount(amount)
                .balanceAmount(newBalance)
                .notes(notes != null ? notes : "Labor payment made")
                .build();

        ledgerRepository.save(ledger);
        log.info("Recorded labor payment: Vendor={}, Amount={}, New Balance={}", 
                vendorId, amount, newBalance);
    }

    /**
     * Calculates complete vendor balance
     */
    public VendorBalance calculateVendorBalance(UUID vendorId) {
        Integer boxesIssued = ledgerRepository.sumBoxesIssuedByVendor(vendorId);
        Integer boxesReturned = ledgerRepository.sumBoxesReturnedByVendor(vendorId);
        Integer boxesDamaged = sumDamagedBoxesByVendor(vendorId);
        
        if (boxesIssued == null) boxesIssued = 0;
        if (boxesReturned == null) boxesReturned = 0;
        if (boxesDamaged == null) boxesDamaged = 0;
        
        BigDecimal pendingLaborCost = ledgerRepository.sumLaborCostByVendor(vendorId);
        if (pendingLaborCost == null) pendingLaborCost = BigDecimal.ZERO;

        int boxesPending = boxesIssued - boxesReturned - boxesDamaged;

        return VendorBalance.builder()
                .vendorId(vendorId)
                .boxesIssued(boxesIssued)
                .boxesReturned(boxesReturned)
                .boxesDamaged(boxesDamaged)
                .boxesPending(boxesPending)
                .pendingLaborCost(pendingLaborCost)
                .build();
    }

    /**
     * Gets detailed vendor ledger with all transactions
     */
    public List<VendorLedgerResponse> getVendorLedgerDetails(UUID vendorId) {
        List<VendorLedger> entries = ledgerRepository.findByVendorOrderByDate(vendorId);
        
        return entries.stream().map(entry -> 
            VendorLedgerResponse.builder()
                    .id(entry.getId())
                    .vendorId(entry.getVendor().getId())
                    .vendorName(entry.getVendor().getFullName())
                    .batchId(entry.getBatch() != null ? entry.getBatch().getId() : null)
                    .batchIdCode(entry.getBatch() != null ? entry.getBatch().getBatchId() : null)
                    .transactionType(entry.getTransactionType())
                    .quantity(entry.getQuantity())
                    .amount(entry.getAmount())
                    .balanceBoxes(entry.getBalanceBoxes())
                    .balanceAmount(entry.getBalanceAmount())
                    .notes(entry.getNotes())
                    .createdAt(entry.getCreatedAt())
                    .build()
        ).collect(Collectors.toList());
    }

    /**
     * Reconciles inventory for a batch after gate pass receipt
     */
    @Transactional
    public void reconcileBatchInventory(UUID batchId, UUID vendorId) {
        // Get all allocations for this batch
        List<InventoryAllocation> allocations = allocationRepository.findByBatchId(batchId);
        
        // Get total boxes allocated
        int totalBoxesAllocated = allocations.stream()
                .filter(a -> a.getItem().getCategory().name().equals("BOX"))
                .mapToInt(InventoryAllocation::getQuantity)
                .sum();
        
        // Get total boxes received via gate passes
        Integer totalBoxesReceived = gatePassRepository.sumReceivedBoxesByBatch(batchId);
        if (totalBoxesReceived == null) totalBoxesReceived = 0;
        
        // Get total boxes wasted
        Integer totalBoxesWasted = harvestReportRepository.sumBoxesWastedByBatch(batchId);
        if (totalBoxesWasted == null) totalBoxesWasted = 0;
        
        // Calculate pending boxes
        int boxesPending = totalBoxesAllocated - totalBoxesReceived - totalBoxesWasted;
        
        log.info("Batch {} Inventory Reconciliation: Allocated={}, Received={}, Wasted={}, Pending={}",
                batchId, totalBoxesAllocated, totalBoxesReceived, totalBoxesWasted, boxesPending);
        
        // If there are pending boxes, record them
        if (boxesPending > 0) {
            recordBoxReturn(vendorId, batchId, totalBoxesReceived, 
                    String.format("Auto-reconciliation: %d boxes received", totalBoxesReceived));
            
            if (totalBoxesWasted > 0) {
                recordDamagedBoxes(vendorId, batchId, totalBoxesWasted,
                        String.format("Auto-reconciliation: %d boxes damaged", totalBoxesWasted));
            }
        }
    }

    private Integer sumDamagedBoxesByVendor(UUID vendorId) {
        // This would query the ledger for BOX_DAMAGED transactions
        // For now, returning 0 as placeholder
        return ledgerRepository.findByVendorId(vendorId).stream()
                .filter(l -> "BOX_DAMAGED".equals(l.getTransactionType()))
                .mapToInt(l -> l.getQuantity() != null ? l.getQuantity() : 0)
                .sum();
    }

    // Inner class for vendor balance
    @lombok.Data
    @lombok.Builder
    public static class VendorBalance {
        private UUID vendorId;
        private Integer boxesIssued;
        private Integer boxesReturned;
        private Integer boxesDamaged;
        private Integer boxesPending;
        private BigDecimal pendingLaborCost;
    }
}
