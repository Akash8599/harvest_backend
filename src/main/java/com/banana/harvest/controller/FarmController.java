package com.banana.harvest.controller;

import com.banana.harvest.dto.common.ApiResponse;
import com.banana.harvest.dto.farm.*;
import com.banana.harvest.security.UserPrincipal;
import com.banana.harvest.service.FarmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Farm Management", description = "Farm and batch management APIs")
public class FarmController {

    private final FarmService farmService;

    // Farm endpoints
    @PostMapping("/farms")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Create farm", description = "Register a new farm")
    public ResponseEntity<ApiResponse<FarmResponse>> createFarm(
            @Valid @RequestBody FarmRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Creating farm - farmerName: {}, location: {}, createdBy: {}",
                request.getFarmerName(), request.getLocation(), userPrincipal.getId());
        FarmResponse response = farmService.createFarm(request, userPrincipal.getId());
        log.info("Farm created successfully - farmId: {}, farmerName: {}", response.getId(), response.getFarmerName());
        return ResponseEntity.ok(ApiResponse.success("Farm created successfully", response));
    }

    @GetMapping("/farms")
    @Operation(summary = "Get all farms", description = "Get list of all farms")
    public ResponseEntity<ApiResponse<List<FarmResponse>>> getAllFarms() {
        List<FarmResponse> response = farmService.getAllFarms();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/farms/paged")
    @Operation(summary = "Get farms (paged)", description = "Get paginated list of farms")
    public ResponseEntity<ApiResponse<Page<FarmResponse>>> getFarmsPaged(Pageable pageable) {
        Page<FarmResponse> response = farmService.getFarmsPaged(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/farms/{id}")
    @Operation(summary = "Get farm by ID", description = "Get farm details by ID")
    public ResponseEntity<ApiResponse<FarmResponse>> getFarmById(@PathVariable UUID id) {
        FarmResponse response = farmService.getFarmById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Inspection endpoints
    @PostMapping("/inspections")
    @PreAuthorize("hasRole('VENDOR') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Create inspection", description = "Submit farm inspection with photos")
    public ResponseEntity<ApiResponse<FarmInspectionResponse>> createInspection(
            @Valid @RequestBody FarmInspectionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Creating inspection - farmId: {}, vendorId: {}, estimatedBoxes: {}",
                request.getFarmId(), userPrincipal.getId(), request.getEstimatedBoxes());
        FarmInspectionResponse response = farmService.createInspection(request, userPrincipal.getId());
        log.info("Inspection created successfully - inspectionId: {}, farmId: {}", response.getId(),
                response.getFarmId());
        return ResponseEntity.ok(ApiResponse.success("Inspection submitted successfully", response));
    }

    @GetMapping("/inspections")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Get all inspections", description = "Get list of all inspections (History)")
    public ResponseEntity<ApiResponse<List<FarmInspectionResponse>>> getAllInspections() {
        log.debug("Fetching all inspections history");
        List<FarmInspectionResponse> response = farmService.getAllInspections();
        log.info("Retrieved {} inspections history", response.size());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/inspections/{id}")
    @PreAuthorize("hasRole('VENDOR') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Get inspection details", description = "Get detailed inspection information")
    public ResponseEntity<ApiResponse<FarmInspectionResponse>> getInspectionById(@PathVariable UUID id) {
        FarmInspectionResponse response = farmService.getInspectionById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/inspections/pending")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Get pending inspections", description = "Get all pending inspections for approval")
    public ResponseEntity<ApiResponse<List<FarmInspectionResponse>>> getPendingInspections() {
        log.debug("Fetching pending inspections");
        List<FarmInspectionResponse> response = farmService.getPendingInspections();
        log.info("Retrieved {} pending inspections", response.size());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/inspections/my")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Get my inspections", description = "Get inspections submitted by current vendor")
    public ResponseEntity<ApiResponse<List<FarmInspectionResponse>>> getMyInspections(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<FarmInspectionResponse> response = farmService.getVendorInspections(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/inspections/pending-count")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Get pending inspection count", description = "Get count of actionable inspections for vendor badge")
    public ResponseEntity<ApiResponse<Long>> getPendingInspectionCount(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        long count = farmService.getPendingInspectionCount(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PostMapping("/inspections/{id}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Approve/Reject inspection", description = "Approve or reject a farm inspection")
    public ResponseEntity<ApiResponse<BatchResponse>> approveInspection(
            @PathVariable UUID id,
            @Valid @RequestBody ApprovalRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Processing inspection approval - inspectionId: {}, approved: {}, approverId: {}",
                id, request.getApproved(), userPrincipal.getId());
        BatchResponse response = farmService.approveInspection(id, request, userPrincipal.getId());
        log.info("Inspection processed - inspectionId: {}, batchId: {}", id, response.getBatchId());
        return ResponseEntity.ok(ApiResponse.success("Inspection processed successfully", response));
    }

    // Batch endpoints
    // Batch endpoints - DEPRECATED: Moved to BatchController
    // @Deprecated
    // @GetMapping("/batches")
    // @Operation(summary = "Get all batches (Deprecated)", description = "Get list of all harvest batches. Use BatchController for detailed batch operations.")
    // public ResponseEntity<ApiResponse<List<BatchResponse>>> getAllBatches() {
    //     List<BatchResponse> response = farmService.getAllBatches();
    //     return ResponseEntity.ok(ApiResponse.success(response));
    // }

    // @Deprecated
    // @GetMapping("/batches/{id}")
    // @Operation(summary = "Get batch by ID (Deprecated)", description = "Get batch details by ID. Use /api/batches/{id} instead for richer data.")
    // public ResponseEntity<ApiResponse<BatchResponse>> getBatchById(@PathVariable UUID id) {
    //     BatchResponse response = farmService.getBatchById(id);
    //     return ResponseEntity.ok(ApiResponse.success(response));
    // }

    // Inspection Request endpoints
    @PostMapping("/inspections/requests")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Create inspection request", description = "Create a new inspection request for a vendor")
    public ResponseEntity<ApiResponse<InspectionRequestResponse>> createInspectionRequest(
            @Valid @RequestBody InspectionRequestDto request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Creating inspection request - farmId: {}, vendorId: {}, createdBy: {}",
                request.getFarmId(), request.getVendorId(), userPrincipal.getId());
        InspectionRequestResponse response = farmService.createInspectionRequest(request, userPrincipal.getId());
        log.info("Inspection request created - requestId: {}, farmId: {}, vendorId: {}",
                response.getId(), response.getFarmId(), response.getVendorId());
        return ResponseEntity.ok(ApiResponse.success("Inspection request created successfully", response));
    }

    @GetMapping("/inspections/requests")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Get all inspection requests", description = "Get list of all inspection requests (Admin/Manager)")
    public ResponseEntity<ApiResponse<List<InspectionRequestResponse>>> getAllInspectionRequests(
            @RequestParam(required = false) String status) {
        log.info("Fetching all inspection requests, status: {}", status);
        List<InspectionRequestResponse> response = farmService.getAllInspectionRequests(status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/inspections/requests/my")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Get my inspection requests", description = "Get inspection requests assigned to current vendor")
    public ResponseEntity<ApiResponse<List<InspectionRequestResponse>>> getMyInspectionRequests(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Fetching inspection requests for vendor: {}, status: {}", userPrincipal.getId(), status);
        List<InspectionRequestResponse> response = farmService.getVendorInspectionRequests(userPrincipal.getId(),
                status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/inspections/requests/{id}/cancel")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Cancel inspection request", description = "Cancel a PENDING inspection request")
    public ResponseEntity<ApiResponse<InspectionRequestResponse>> cancelInspectionRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Cancelling inspection request - requestId: {}, cancelledBy: {}", id, userPrincipal.getId());
        InspectionRequestResponse response = farmService.cancelInspectionRequest(id, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Request cancelled successfully", response));
    }
}
