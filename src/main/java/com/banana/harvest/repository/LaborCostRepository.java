package com.banana.harvest.repository;

import com.banana.harvest.entity.LaborCost;
import com.banana.harvest.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface LaborCostRepository extends JpaRepository<LaborCost, UUID> {
    
    List<LaborCost> findByBatchId(UUID batchId);
    
    @Query("SELECT lc FROM LaborCost lc WHERE lc.batch.id = :batchId")
    List<LaborCost> findByBatch(@Param("batchId") UUID batchId);
    
    @Query("SELECT COALESCE(SUM(lc.totalAmount), 0) FROM LaborCost lc WHERE lc.batch.id = :batchId")
    BigDecimal sumTotalAmountByBatch(@Param("batchId") UUID batchId);
    
    @Query("SELECT COALESCE(SUM(lc.totalAmount), 0) FROM LaborCost lc WHERE lc.batch.vendor.id = :vendorId AND lc.paymentStatus = 'PENDING'")
    BigDecimal sumPendingAmountByVendor(@Param("vendorId") UUID vendorId);
    
    List<LaborCost> findByPaymentStatus(PaymentStatus status);
}
