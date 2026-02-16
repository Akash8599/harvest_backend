package com.banana.harvest.controller;

import com.banana.harvest.dto.common.ApiResponse;
import com.banana.harvest.dto.cost.BatchCostResponse;
import com.banana.harvest.service.CostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/costs")
@RequiredArgsConstructor
@Tag(name = "Cost Management", description = "Batch cost calculation and viewing APIs")
public class CostController {

    private final CostService costService;

    @GetMapping("/batch/{batchId}")
    @Operation(summary = "Get batch cost", description = "Get cost breakdown for a batch")
    public ResponseEntity<ApiResponse<BatchCostResponse>> getBatchCost(@PathVariable UUID batchId) {
        BatchCostResponse response = costService.getBatchCost(batchId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/batch/code/{batchId}")
    @Operation(summary = "Get batch cost by code", description = "Get cost breakdown using batch ID code")
    public ResponseEntity<ApiResponse<BatchCostResponse>> getCostByBatchCode(@PathVariable String batchId) {
        BatchCostResponse response = costService.getCostByBatchCode(batchId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Get all batch costs", description = "Get cost breakdown for all batches")
    public ResponseEntity<ApiResponse<List<BatchCostResponse>>> getAllBatchCosts() {
        List<BatchCostResponse> response = costService.getAllBatchCosts();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
