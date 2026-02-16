package com.banana.harvest.controller;

import com.banana.harvest.dto.common.ApiResponse;
import com.banana.harvest.dto.sales.SaleRequest;
import com.banana.harvest.dto.sales.SaleResponse;
import com.banana.harvest.security.UserPrincipal;
import com.banana.harvest.service.SalesService;
import com.banana.harvest.service.PdfInvoiceService;
import com.banana.harvest.service.InvoiceSharingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@Tag(name = "Sales Management", description = "Sales and invoicing APIs")
public class SalesController {

    private final SalesService salesService;
    private final PdfInvoiceService pdfInvoiceService;
    private final InvoiceSharingService invoiceSharingService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Create sale", description = "Create a new sale and generate invoice")
    public ResponseEntity<ApiResponse<SaleResponse>> createSale(
            @Valid @RequestBody SaleRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Creating sale - batchId: {}, buyerName: {}, totalBoxes: {}, pricePerBox: {}, createdBy: {}", 
                request.getBatchId(), request.getBuyerName(), request.getTotalBoxes(), request.getPricePerBox(), userPrincipal.getId());
        SaleResponse response = salesService.createSale(request, userPrincipal.getId());
        log.info("Sale created - saleId: {}, invoiceNumber: {}, totalAmount: {}, paymentStatus: {}", 
                response.getId(), response.getInvoiceNumber(), response.getTotalAmount(), response.getPaymentStatus());
        return ResponseEntity.ok(ApiResponse.success("Sale created successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Get all sales", description = "Get list of all sales")
    public ResponseEntity<ApiResponse<List<SaleResponse>>> getAllSales() {
        List<SaleResponse> response = salesService.getAllSales();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/paged")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Get sales (paged)", description = "Get paginated list of sales")
    public ResponseEntity<ApiResponse<Page<SaleResponse>>> getSalesPaged(Pageable pageable) {
        Page<SaleResponse> response = salesService.getSalesPaged(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Get sale by ID", description = "Get sale details by ID")
    public ResponseEntity<ApiResponse<SaleResponse>> getSaleById(@PathVariable UUID id) {
        SaleResponse response = salesService.getSaleById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/invoice/{invoiceNumber}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Get sale by invoice", description = "Get sale details by invoice number")
    public ResponseEntity<ApiResponse<SaleResponse>> getSaleByInvoiceNumber(@PathVariable String invoiceNumber) {
        SaleResponse response = salesService.getSaleByInvoiceNumber(invoiceNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/batch/{batchId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Get batch sales", description = "Get all sales for a batch")
    public ResponseEntity<ApiResponse<List<SaleResponse>>> getBatchSales(@PathVariable UUID batchId) {
        List<SaleResponse> response = salesService.getBatchSales(batchId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/payment")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Update payment status", description = "Update payment status for a sale")
    public ResponseEntity<ApiResponse<Void>> updatePaymentStatus(
            @PathVariable UUID id,
            @RequestParam String status,
            @RequestParam(required = false) BigDecimal amount) {
        log.info("Updating payment status - saleId: {}, newStatus: {}, amount: {}", id, status, amount);
        salesService.updatePaymentStatus(id, status, amount);
        log.info("Payment status updated successfully - saleId: {}, status: {}", id, status);
        return ResponseEntity.ok(ApiResponse.success("Payment status updated successfully", null));
    }

    @GetMapping("/{id}/invoice/pdf")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Download invoice PDF", description = "Generate and download invoice PDF")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable UUID id) {
        log.info("Generating invoice PDF - saleId: {}", id);
        byte[] pdfBytes = pdfInvoiceService.generateInvoice(id);
        log.info("Invoice PDF generated successfully - saleId: {}, size: {} bytes", id, pdfBytes.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PostMapping("/{id}/invoice/share/whatsapp")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Share invoice via WhatsApp", description = "Share invoice PDF via WhatsApp")
    public ResponseEntity<ApiResponse<Void>> shareInvoiceViaWhatsApp(
            @PathVariable UUID id,
            @RequestParam String phoneNumber) {
        log.info("Sharing invoice via WhatsApp - saleId: {}, phoneNumber: {}", id, phoneNumber);
        invoiceSharingService.shareViaWhatsApp(id, phoneNumber);
        log.info("Invoice shared via WhatsApp successfully - saleId: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Invoice shared via WhatsApp", null));
    }

    @PostMapping("/{id}/invoice/share/email")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Share invoice via Email", description = "Share invoice PDF via Email")
    public ResponseEntity<ApiResponse<Void>> shareInvoiceViaEmail(
            @PathVariable UUID id,
            @RequestParam String email) {
        invoiceSharingService.shareViaEmail(id, email);
        return ResponseEntity.ok(ApiResponse.success("Invoice shared via Email", null));
    }

    @GetMapping("/{id}/invoice/share/whatsapp-link")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Get WhatsApp share link", description = "Generate WhatsApp Click-to-Chat link")
    public ResponseEntity<ApiResponse<String>> getWhatsAppShareLink(
            @PathVariable UUID id,
            @RequestParam String phoneNumber) {
        String link = invoiceSharingService.generateWhatsAppShareLink(id, phoneNumber);
        return ResponseEntity.ok(ApiResponse.success(link));
    }
}
