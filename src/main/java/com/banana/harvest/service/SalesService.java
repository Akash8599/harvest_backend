package com.banana.harvest.service;

import com.banana.harvest.dto.sales.SaleRequest;
import com.banana.harvest.dto.sales.SaleResponse;
import com.banana.harvest.entity.Batch;
import com.banana.harvest.entity.BatchCost;
import com.banana.harvest.entity.Sale;
import com.banana.harvest.entity.enums.BatchStatus;
import com.banana.harvest.exception.BusinessException;
import com.banana.harvest.exception.ResourceNotFoundException;
import com.banana.harvest.repository.BatchCostRepository;
import com.banana.harvest.repository.BatchRepository;
import com.banana.harvest.repository.SaleRepository;
import com.banana.harvest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final SaleRepository saleRepository;
    private final BatchRepository batchRepository;
    private final BatchCostRepository batchCostRepository;
    private final UserRepository userRepository;

    @Transactional
    public SaleResponse createSale(SaleRequest request, UUID userId) {
        Batch batch = batchRepository.findById(UUID.fromString(request.getBatchId()))
                .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", request.getBatchId()));

        // Check if batch is completed
        if (batch.getStatus() != BatchStatus.COMPLETED) {
            throw new BusinessException("Batch must be completed before creating a sale");
        }

        // Check if batch already has a sale
        List<Sale> existingSales = saleRepository.findByBatchId(batch.getId());
        if (!existingSales.isEmpty()) {
            throw new BusinessException("Sale already exists for this batch");
        }

        // Validate sale quantity
        if (request.getTotalBoxes() > batch.getActualBoxes()) {
            throw new BusinessException("Sale quantity cannot exceed actual boxes harvested");
        }

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Calculate totals
        BigDecimal totalAmount = request.getPricePerBox().multiply(BigDecimal.valueOf(request.getTotalBoxes()));
        
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal grandTotal = totalAmount;
        
        if (request.getTaxPercentage() != null && request.getTaxPercentage().compareTo(BigDecimal.ZERO) > 0) {
            taxAmount = totalAmount.multiply(request.getTaxPercentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            grandTotal = totalAmount.add(taxAmount);
        }

        String invoiceNumber = generateInvoiceNumber();

        Sale sale = Sale.builder()
                .batch(batch)
                .invoiceNumber(invoiceNumber)
                .buyerName(request.getBuyerName())
                .buyerContact(request.getBuyerContact())
                .buyerAddress(request.getBuyerAddress())
                .saleType(request.getSaleType())
                .totalBoxes(request.getTotalBoxes())
                .pricePerBox(request.getPricePerBox())
                .currency(request.getCurrency())
                .exchangeRate(request.getExchangeRate() != null ? request.getExchangeRate() : BigDecimal.ONE)
                .totalAmount(totalAmount)
                .taxAmount(taxAmount)
                .taxPercentage(request.getTaxPercentage())
                .grandTotal(grandTotal)
                .saleDate(request.getSaleDate())
                .createdBy(user)
                .build();

        Sale savedSale = saleRepository.save(sale);
        return mapToSaleResponse(savedSale);
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> getAllSales() {
        return saleRepository.findAll().stream()
                .map(this::mapToSaleResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<SaleResponse> getSalesPaged(Pageable pageable) {
        return saleRepository.findAll(pageable)
                .map(this::mapToSaleResponse);
    }

    @Transactional(readOnly = true)
    public SaleResponse getSaleById(UUID id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", "id", id));
        return mapToSaleResponse(sale);
    }

    @Transactional(readOnly = true)
    public SaleResponse getSaleByInvoiceNumber(String invoiceNumber) {
        Sale sale = saleRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", "invoiceNumber", invoiceNumber));
        return mapToSaleResponse(sale);
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> getBatchSales(UUID batchId) {
        return saleRepository.findByBatchId(batchId).stream()
                .map(this::mapToSaleResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updatePaymentStatus(UUID saleId, String status, BigDecimal amount) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", "id", saleId));

        sale.setPaymentStatus(com.banana.harvest.entity.enums.PaymentStatus.valueOf(status));
        
        if (amount != null) {
            sale.setPaidAmount(sale.getPaidAmount().add(amount));
        }

        saleRepository.save(sale);
    }

    private String generateInvoiceNumber() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = saleRepository.count() + 1;
        return "INV-" + datePrefix + "-" + String.format("%05d", count);
    }

    private SaleResponse mapToSaleResponse(Sale sale) {
        return SaleResponse.builder()
                .id(sale.getId())
                .batchId(sale.getBatch().getId())
                .batchIdCode(sale.getBatch().getBatchId())
                .invoiceNumber(sale.getInvoiceNumber())
                .buyerName(sale.getBuyerName())
                .buyerContact(sale.getBuyerContact())
                .buyerAddress(sale.getBuyerAddress())
                .saleType(sale.getSaleType())
                .totalBoxes(sale.getTotalBoxes())
                .pricePerBox(sale.getPricePerBox())
                .currency(sale.getCurrency())
                .exchangeRate(sale.getExchangeRate())
                .totalAmount(sale.getTotalAmount())
                .taxAmount(sale.getTaxAmount())
                .taxPercentage(sale.getTaxPercentage())
                .grandTotal(sale.getGrandTotal())
                .invoiceUrl(sale.getInvoiceUrl())
                .paymentStatus(sale.getPaymentStatus())
                .paidAmount(sale.getPaidAmount())
                .saleDate(sale.getSaleDate())
                .createdAt(sale.getCreatedAt())
                .build();
    }
}
