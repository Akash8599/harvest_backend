package com.banana.harvest.controller;

import com.banana.harvest.dto.common.ApiResponse;
import com.banana.harvest.dto.harvest.GatePassRequest;
import com.banana.harvest.dto.harvest.GatePassResponse;
import com.banana.harvest.security.UserPrincipal;
import com.banana.harvest.service.HarvestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/gate-passes")
@RequiredArgsConstructor
@Tag(name = "Gate Pass Management", description = "Gate pass and dispatch management APIs")
public class GatePassController {

    private final HarvestService harvestService;

    @PostMapping
    @PreAuthorize("hasRole('VENDOR') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Create gate pass", description = "Generate gate pass for truck dispatch")
    public ResponseEntity<ApiResponse<GatePassResponse>> createGatePass(
            @Valid @RequestBody GatePassRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Creating gate pass - batchId: {}, truckNumber: {}, totalBoxes: {}, createdBy: {}",
                request.getBatchId(), request.getTruckNumber(), request.getTotalBoxes(), userPrincipal.getId());
        GatePassResponse response = harvestService.createGatePass(request, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Gate pass created successfully", response));
    }

    @GetMapping("/today")
    @Operation(summary = "Get today's gate passes", description = "Get all gate passes created today")
    public ResponseEntity<ApiResponse<List<GatePassResponse>>> getTodayGatePasses() {
        List<GatePassResponse> response = harvestService.getGatePassesByDate(LocalDate.now());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/reports")
    @Operation(summary = "Get gate passes by date", description = "Get gate passes for a specific date")
    public ResponseEntity<ApiResponse<List<GatePassResponse>>> getGatePassReports(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<GatePassResponse> response = harvestService.getGatePassesByDate(date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/batch/{batchId}")
    @Operation(summary = "Get batch gate passes", description = "Get all gate passes for a batch")
    public ResponseEntity<ApiResponse<List<GatePassResponse>>> getBatchGatePasses(@PathVariable UUID batchId) {
        List<GatePassResponse> response = harvestService.getBatchGatePasses(batchId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER') or hasRole('STORE_KEEPER')")
    @Operation(summary = "Get pending gate passes", description = "Get all gate passes that haven't been received yet")
    public ResponseEntity<ApiResponse<List<GatePassResponse>>> getPendingGatePasses() {
        List<GatePassResponse> response = harvestService.getPendingGatePasses();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasRole('STORE_KEEPER') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Receive gate pass", description = "Record receipt of goods at cold storage")
    public ResponseEntity<ApiResponse<GatePassResponse>> receiveGatePass(
            @PathVariable UUID id,
            @RequestParam Integer receivedBoxes,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Receiving gate pass - gatePassId: {}, receivedBoxes: {}, receivedBy: {}",
                id, receivedBoxes, userPrincipal.getId());
        GatePassResponse response = harvestService.receiveGatePass(id, receivedBoxes, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Gate pass received successfully", response));
    }
}
