package com.banana.harvest.controller;

import com.banana.harvest.dto.common.ApiResponse;
import com.banana.harvest.dto.harvest.*;
import com.banana.harvest.entity.GatePass;
import com.banana.harvest.security.UserPrincipal;
import com.banana.harvest.service.HarvestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Harvest Management", description = "Daily harvest and transport management APIs")
public class HarvestController {

    private final HarvestService harvestService;

    // Daily harvest endpoints
    @PostMapping("/harvest/daily")
    @PreAuthorize("hasRole('VENDOR') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Create daily report", description = "Submit daily harvest report")
    public ResponseEntity<ApiResponse<DailyHarvestResponse>> createDailyReport(
            @Valid @RequestBody DailyHarvestRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Creating daily harvest report - batchId: {}, reportDate: {}, boxesPacked: {}, reportedBy: {}",
                request.getBatchId(), request.getReportDate(), request.getBoxesPacked(), userPrincipal.getId());
        DailyHarvestResponse response = harvestService.createDailyReport(request, userPrincipal.getId());
        log.info("Daily report created - reportId: {}, batchId: {}, boxesPacked: {}",
                response.getId(), response.getBatchId(), response.getBoxesPacked());
        return ResponseEntity.ok(ApiResponse.success("Daily report created successfully", response));
    }

    @GetMapping("/harvest/batch/{batchId}")
    @Operation(summary = "Get batch reports", description = "Get all daily reports for a batch")
    public ResponseEntity<ApiResponse<List<DailyHarvestResponse>>> getBatchReports(@PathVariable UUID batchId) {
        List<DailyHarvestResponse> response = harvestService.getBatchReports(batchId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/harvest/today")
    @Operation(summary = "Get today's reports", description = "Get all harvest reports for today")
    public ResponseEntity<ApiResponse<List<DailyHarvestResponse>>> getTodayReports() {
        List<DailyHarvestResponse> response = harvestService.getReportsByDate(LocalDate.now());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/harvest/reports")
    @Operation(summary = "Get harvest reports by date", description = "Get harvest reports for a specific date")
    public ResponseEntity<ApiResponse<List<DailyHarvestResponse>>> getHarvestReportsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<DailyHarvestResponse> response = harvestService.getReportsByDate(date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Transport cost endpoints
    @PostMapping("/transport")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Add transport cost", description = "Record transport cost for a batch")
    public ResponseEntity<ApiResponse<Void>> addTransportCost(
            @Valid @RequestBody TransportCostRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        harvestService.addTransportCost(request, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Transport cost added successfully", null));
    }
}
