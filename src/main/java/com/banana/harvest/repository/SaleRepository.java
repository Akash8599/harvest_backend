package com.banana.harvest.repository;

import com.banana.harvest.entity.Sale;
import com.banana.harvest.entity.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SaleRepository extends JpaRepository<Sale, UUID> {
    
    Optional<Sale> findByInvoiceNumber(String invoiceNumber);
    
    List<Sale> findByBatchId(UUID batchId);
    
    Page<Sale> findByPaymentStatus(PaymentStatus status, Pageable pageable);
    
    @Query("SELECT s FROM Sale s WHERE s.saleDate BETWEEN :startDate AND :endDate")
    List<Sale> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COALESCE(SUM(s.grandTotal), 0) FROM Sale s")
    BigDecimal sumTotalRevenue();
    
    @Query("SELECT COALESCE(SUM(s.grandTotal), 0) FROM Sale s WHERE s.saleDate BETWEEN :startDate AND :endDate")
    BigDecimal sumRevenueByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COALESCE(AVG(s.pricePerBox), 0) FROM Sale s")
    BigDecimal averageSalePrice();
    
    @Query("SELECT COALESCE(SUM(s.totalBoxes), 0) FROM Sale s")
    Integer sumTotalBoxesSold();
}
