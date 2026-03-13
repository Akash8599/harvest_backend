package com.banana.harvest.controller;

import com.banana.harvest.dto.coldstorage.*;
import com.banana.harvest.dto.common.ApiResponse;
import com.banana.harvest.security.UserPrincipal;
import com.banana.harvest.service.ColdStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/cold-storage")
@RequiredArgsConstructor
@Tag(name = "Cold Storage", description = "Cold storage inward, outward, and inventory APIs")
public class ColdStorageController {

    private final ColdStorageService coldStorageService;

    @PostMapping("/inward")
    @PreAuthorize("hasRole('STORE_KEEPER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create inward record", description = "Log harvest boxes arriving at cold storage (Store keeper or Super Admin)")
    public ResponseEntity<ApiResponse<ColdStorageInwardResponse>> createInward(
            @Valid @RequestBody ColdStorageInwardRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to create Cold Storage Inward by user: {}", userPrincipal.getId());
        ColdStorageInwardResponse response = coldStorageService.createInward(request, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Inward record created successfully", response));
    }

    @GetMapping("/inward")
    @PreAuthorize("hasRole('STORE_KEEPER') or hasRole('MANAGER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get all inward records", description = "List all cold storage inward logs")
    public ResponseEntity<ApiResponse<List<ColdStorageInwardResponse>>> getAllInwards() {
        log.info("REST request to get all Cold Storage Inwards");
        List<ColdStorageInwardResponse> response = coldStorageService.getAllInwards();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/inward/batch/{batchId}")
    @Operation(summary = "Get inward records by batch", description = "List cold storage inward logs for a specific batch UUID")
    public ResponseEntity<ApiResponse<List<ColdStorageInwardResponse>>> getInwardsByBatch(
            @PathVariable UUID batchId) {
        log.info("REST request to get Cold Storage Inwards for batch: {}", batchId);
        List<ColdStorageInwardResponse> response = coldStorageService.getInwardsByBatch(batchId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/outward")
    @PreAuthorize("hasRole('STORE_KEEPER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create outward record", description = "Log boxes dispatched from cold storage (Max 1540 boxes)")
    public ResponseEntity<ApiResponse<ColdStorageOutwardResponse>> createOutward(
            @Valid @RequestBody ColdStorageOutwardRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to create Cold Storage Outward by user: {}", userPrincipal.getId());
        ColdStorageOutwardResponse response = coldStorageService.createOutward(request, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Outward record created successfully", response));
    }

    @GetMapping("/outward")
    @Operation(summary = "Get all outward records", description = "List all cold storage outward dispatches")
    public ResponseEntity<ApiResponse<List<ColdStorageOutwardResponse>>> getAllOutwards() {
        log.info("REST request to get all Cold Storage Outwards");
        List<ColdStorageOutwardResponse> response = coldStorageService.getAllOutwards();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/inventory")
    @Operation(summary = "Get current inventory", description = "Get aggregate totals and balance of boxes in cold storage")
    public ResponseEntity<ApiResponse<ColdStorageInventoryResponse>> getInventory() {
        log.info("REST request to get Cold Storage Inventory balance");
        ColdStorageInventoryResponse response = coldStorageService.getInventory();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
