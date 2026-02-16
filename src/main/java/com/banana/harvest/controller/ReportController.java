package com.banana.harvest.controller;

import com.banana.harvest.dto.common.ApiResponse;
import com.banana.harvest.dto.report.*;
import com.banana.harvest.security.UserPrincipal;
import com.banana.harvest.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports & Dashboard", description = "Dashboard and reporting APIs")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard stats", description = "Get key metrics for dashboard")
    public ResponseEntity<ApiResponse<DashboardStats>> getDashboardStats() {
        DashboardStats response = reportService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/vendor-ledger/{vendorId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER') or @securityService.isCurrentUser(#vendorId)")
    @Operation(summary = "Get vendor ledger", description = "Get transaction history for a vendor")
    public ResponseEntity<ApiResponse<List<VendorLedgerResponse>>> getVendorLedger(@PathVariable UUID vendorId) {
        List<VendorLedgerResponse> response = reportService.getVendorLedger(vendorId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/vendor-balance/{vendorId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER') or @securityService.isCurrentUser(#vendorId)")
    @Operation(summary = "Get vendor balance", description = "Get current balance for a vendor")
    public ResponseEntity<ApiResponse<ReportService.VendorBalance>> getVendorBalance(@PathVariable UUID vendorId) {
        ReportService.VendorBalance response = reportService.getVendorBalance(vendorId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/profitability")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Get profitability report", description = "Get profit analysis for all sales")
    public ResponseEntity<ApiResponse<List<ProfitabilityReportResponse>>> getProfitabilityReport() {
        List<ProfitabilityReportResponse> response = reportService.getProfitabilityReport();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/daily-activity")
    @Operation(summary = "Get daily activity", description = "Get today's harvest activity summary")
    public ResponseEntity<ApiResponse<ReportService.DailyActivityReport>> getDailyActivityReport() {
        ReportService.DailyActivityReport response = reportService.getDailyActivityReport();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my-ledger")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Get my ledger", description = "Get current vendor's transaction history")
    public ResponseEntity<ApiResponse<List<VendorLedgerResponse>>> getMyLedger(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<VendorLedgerResponse> response = reportService.getVendorLedger(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my-balance")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Get my balance", description = "Get current vendor's balance")
    public ResponseEntity<ApiResponse<ReportService.VendorBalance>> getMyBalance(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ReportService.VendorBalance response = reportService.getVendorBalance(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
