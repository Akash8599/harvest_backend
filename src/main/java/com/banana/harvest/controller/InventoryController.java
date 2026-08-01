package com.banana.harvest.controller;

import com.banana.harvest.dto.common.ApiResponse;
import com.banana.harvest.dto.inventory.*;
import com.banana.harvest.security.UserPrincipal;
import com.banana.harvest.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory Management", description = "Inventory and stock management APIs")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/items")
    @Operation(summary = "Get all inventory items", description = "Get list of all inventory items")
    public ResponseEntity<ApiResponse<List<InventoryItemResponse>>> getAllItems() {
        List<InventoryItemResponse> response = inventoryService.getAllItems();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/items/{id}")
    @Operation(summary = "Get item by ID", description = "Get inventory item details by ID")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> getItemById(@PathVariable UUID id) {
        InventoryItemResponse response = inventoryService.getItemById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/items")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Create inventory item", description = "Add a new inventory item")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> createItem(
            @Valid @RequestBody InventoryItemRequest request) {
        InventoryItemResponse response = inventoryService.createItem(request);
        return ResponseEntity.ok(ApiResponse.success("Item created successfully", response));
    }

    @PostMapping("/items/{id}/stock")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER') or hasRole('STORE_KEEPER')")
    @Operation(summary = "Adjust stock", description = "Add or remove stock from inventory item (accepts negative values)")
    public ResponseEntity<ApiResponse<Void>> adjustStock(
            @PathVariable UUID id,
            @RequestParam Integer quantity,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        inventoryService.addStock(id, quantity, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Stock adjusted successfully", null));
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Update inventory item", description = "Update basic details of an inventory item")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> updateItem(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInventoryItemRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        InventoryItemResponse response = inventoryService.updateItem(id, request, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Item updated successfully", response));
    }

    @GetMapping("/items/{id}/stock")
    @Operation(summary = "Get available stock", description = "Get available stock for an item")
    public ResponseEntity<ApiResponse<Integer>> getAvailableStock(@PathVariable UUID id) {
        Integer response = inventoryService.getAvailableStock(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/allocate")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Allocate inventory", description = "Allocate inventory items to a batch")
    public ResponseEntity<ApiResponse<Void>> allocateInventory(
            @Valid @RequestBody InventoryAllocationRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        inventoryService.allocateInventory(request, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Inventory allocated successfully", null));
    }
}
