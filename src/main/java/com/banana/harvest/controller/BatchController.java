package com.banana.harvest.controller;

import com.banana.harvest.dto.common.ApiResponse;
import com.banana.harvest.dto.farm.BatchResponse;
import com.banana.harvest.dto.farm.UpdateBatchStatusRequest;
import com.banana.harvest.security.UserPrincipal;
import com.banana.harvest.service.BatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
@Tag(name = "Batch Management", description = "Batch lifecycle management APIs")
public class BatchController {

    private final BatchService batchService;

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('VENDOR') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Get batch details", description = "Get detailed batch information including harvest stats")
    public ResponseEntity<ApiResponse<BatchResponse>> getBatchById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) { // UserPrincipal for potential future checks
        BatchResponse response = batchService.getBatchById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('VENDOR') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Update batch status", description = "Update the status of a batch (e.g. to HARVEST_COMPLETED)")
    public ResponseEntity<ApiResponse<BatchResponse>> updateBatchStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBatchStatusRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Request to update batch status - batchId: {}, status: {}, updatesBy: {}",
                id, request.getStatus(), userPrincipal.getId());

        BatchResponse response = batchService.updateBatchStatus(id, request, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Batch status updated successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all batches (Deprecated)", description = "Get list of all harvest batches. Use BatchController for detailed batch operations.")
    public ResponseEntity<ApiResponse<List<BatchResponse>>> getAllBatches() {
        List<BatchResponse> response = batchService.getAllBatches();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
