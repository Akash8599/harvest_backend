package com.banana.harvest.dto.sales;

import com.banana.harvest.entity.enums.PaymentStatus;
import com.banana.harvest.entity.enums.SaleType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SaleResponse {
    private UUID id;
    private UUID batchId;
    private String batchIdCode;
    private String invoiceNumber;
    private String buyerName;
    private String buyerContact;
    private String buyerAddress;
    private SaleType saleType;
    private Integer totalBoxes;
    private BigDecimal pricePerBox;
    private String currency;
    private BigDecimal exchangeRate;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private BigDecimal taxPercentage;
    private BigDecimal grandTotal;
    private String invoiceUrl;
    private PaymentStatus paymentStatus;
    private BigDecimal paidAmount;
    private LocalDate saleDate;
    private LocalDateTime createdAt;
}
