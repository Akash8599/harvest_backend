package com.banana.harvest.service;

import com.banana.harvest.dto.inventory.*;
import com.banana.harvest.entity.*;
import com.banana.harvest.exception.BusinessException;
import com.banana.harvest.exception.ResourceNotFoundException;
import com.banana.harvest.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository itemRepository;
    private final InventoryStockRepository stockRepository;
    private final InventoryAllocationRepository allocationRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final VendorLedgerRepository ledgerRepository;

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> getAllItems() {
        return itemRepository.findByIsActiveTrue().stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse getItemById(UUID id) {
        InventoryItem item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item", "id", id));
        return mapToItemResponse(item);
    }

    @Transactional
    public InventoryItemResponse createItem(InventoryItemRequest request) {
        if (itemRepository.existsByItemCode(request.getItemCode())) {
            throw new BusinessException("Item code already exists");
        }

        InventoryItem item = InventoryItem.builder()
                .itemName(request.getItemName())
                .itemCode(request.getItemCode())
                .category(request.getCategory())
                .unitOfMeasure(request.getUnitOfMeasure())
                .unitCost(request.getUnitCost())
                .isActive(true)
                .build();

        InventoryItem savedItem = itemRepository.save(item);

        // Create stock entry
        InventoryStock stock = InventoryStock.builder()
                .item(savedItem)
                .totalQuantity(0)
                .availableQuantity(0)
                .reservedQuantity(0)
                .build();
        stockRepository.save(stock);

        return mapToItemResponse(savedItem);
    }

    @Transactional
    public void addStock(UUID itemId, Integer quantity) {
        InventoryItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item", "id", itemId));

        InventoryStock stock = stockRepository.findByItemId(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock", "itemId", itemId));

        stock.setTotalQuantity(stock.getTotalQuantity() + quantity);
        stock.setAvailableQuantity(stock.getAvailableQuantity() + quantity);
        stockRepository.save(stock);
    }

    @Transactional
    public void allocateInventory(InventoryAllocationRequest request, UUID allocatedById) {
        Batch batch = batchRepository.findById(UUID.fromString(request.getBatchId()))
                .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", request.getBatchId()));

        InventoryItem item = itemRepository.findById(UUID.fromString(request.getItemId()))
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item", "id", request.getItemId()));

        User allocatedBy = userRepository.findById(allocatedById)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", allocatedById));

        InventoryStock stock = stockRepository.findByItemId(item.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Stock", "itemId", item.getId()));

        if (stock.getAvailableQuantity() < request.getQuantity()) {
            throw new BusinessException("Insufficient stock available. Available: " + 
                    stock.getAvailableQuantity() + ", Requested: " + request.getQuantity());
        }

        // Update stock
        stock.setAvailableQuantity(stock.getAvailableQuantity() - request.getQuantity());
        stock.setReservedQuantity(stock.getReservedQuantity() + request.getQuantity());
        stockRepository.save(stock);

        // Create allocation
        InventoryAllocation allocation = InventoryAllocation.builder()
                .batch(batch)
                .item(item)
                .quantity(request.getQuantity())
                .allocatedBy(allocatedBy)
                .notes(request.getNotes())
                .build();
        allocationRepository.save(allocation);

        // Update vendor ledger for boxes
        if (item.getCategory().name().equals("BOX")) {
            VendorLedger ledger = VendorLedger.builder()
                    .vendor(batch.getVendor())
                    .batch(batch)
                    .transactionType("BOX_ISSUED")
                    .quantity(request.getQuantity())
                    .notes("Allocated " + item.getItemName())
                    .build();
            ledgerRepository.save(ledger);
        }
    }

    @Transactional(readOnly = true)
    public List<InventoryAllocation> getBatchAllocations(UUID batchId) {
        return allocationRepository.findByBatchId(batchId);
    }

    @Transactional(readOnly = true)
    public Integer getAvailableStock(UUID itemId) {
        return stockRepository.findByItemId(itemId)
                .map(InventoryStock::getAvailableQuantity)
                .orElse(0);
    }

    private InventoryItemResponse mapToItemResponse(InventoryItem item) {
        Integer availableQty = stockRepository.findByItemId(item.getId())
                .map(InventoryStock::getAvailableQuantity)
                .orElse(0);

        return InventoryItemResponse.builder()
                .id(item.getId())
                .itemName(item.getItemName())
                .itemCode(item.getItemCode())
                .category(item.getCategory())
                .unitOfMeasure(item.getUnitOfMeasure())
                .unitCost(item.getUnitCost())
                .isActive(item.getIsActive())
                .availableQuantity(availableQty)
                .createdAt(item.getCreatedAt())
                .build();
    }
}
